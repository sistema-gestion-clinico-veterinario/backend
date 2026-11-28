package veterinaria.vargasvet.realtime;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
public class EscuchaPostgres {

    public enum Estado { DETENIDA, CONECTANDO, SIN_VERIFICAR, ESCUCHANDO }

    private static final Pattern CANAL_VALIDO = Pattern.compile("[a-z_][a-z0-9_]{0,62}");
    private static final long ESPERA_NOTIFICACIONES_MS = 1000;
    private static final long LATIDO_MS = 30_000;
    private static final long PLAZO_SONDA_MS = 10_000;
    private static final long ESPERA_INICIAL_MS = 1000;
    private static final long ESPERA_MAXIMA_MS = 30_000;

    private final String url;
    private final String usuario;
    private final String clave;
    private final Properties propiedades;
    private final String canal;
    private final String origen;
    private final Consumer<SobreTiempoReal> entrega;

    private volatile boolean activa;
    private volatile Connection conexion;
    private volatile Estado estado = Estado.DETENIDA;
    private Thread hilo;
    private long espera = ESPERA_INICIAL_MS;

    public EscuchaPostgres(String url, String usuario, String clave, Properties propiedades, String canal,
                           String origen, Consumer<SobreTiempoReal> entrega) {
        if (!CANAL_VALIDO.matcher(canal).matches()) {
            throw new IllegalArgumentException("Nombre de canal no válido");
        }
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        this.propiedades = propiedades == null ? new Properties() : propiedades;
        this.canal = canal;
        this.origen = origen;
        this.entrega = entrega;
    }

    public synchronized void iniciar() {
        if (activa) {
            return;
        }
        activa = true;
        estado = Estado.CONECTANDO;
        hilo = new Thread(this::ciclo, "tiempo-real-escucha");
        hilo.setDaemon(true);
        hilo.start();
    }

    public synchronized void detener() {
        activa = false;
        Connection actual = conexion;
        if (actual != null) {
            try {
                actual.close();
            } catch (Exception ignorada) {
                log.debug("Cierre de la conexión de escucha con error");
            }
        }
        if (hilo != null) {
            hilo.interrupt();
            try {
                hilo.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            hilo = null;
        }
        estado = Estado.DETENIDA;
    }

    public Estado estado() {
        return estado;
    }

    public String nombreDeAplicacion() {
        return "vetsoft-tiempo-real-" + origen.substring(0, Math.min(8, origen.length()));
    }

    private void ciclo() {
        while (activa) {
            try {
                escuchar();
            } catch (Exception e) {
                if (!activa) {
                    break;
                }
                estado = Estado.CONECTANDO;
                log.warn("Escucha de tiempo real interrumpida; se reintenta en {} ms: {}", espera,
                        e.getClass().getSimpleName());
            }
            if (!dormir(espera)) {
                break;
            }
            espera = Math.min(espera * 2, ESPERA_MAXIMA_MS);
        }
        estado = Estado.DETENIDA;
    }

    private void escuchar() throws Exception {
        try (Connection abierta = abrir()) {
            conexion = abierta;
            try (Statement sentencia = abierta.createStatement()) {
                sentencia.execute("LISTEN " + canal);
            }
            PGConnection postgres = abierta.unwrap(PGConnection.class);
            espera = ESPERA_INICIAL_MS;
            estado = Estado.SIN_VERIFICAR;
            enviarSonda(abierta);
            long inicio = System.nanoTime();
            long ultimoLatido = inicio;
            boolean avisado = false;
            while (activa) {
                PGNotification[] notificaciones = postgres.getNotifications((int) ESPERA_NOTIFICACIONES_MS);
                if (notificaciones != null) {
                    for (PGNotification notificacion : notificaciones) {
                        procesar(notificacion.getParameter());
                    }
                }
                long ahora = System.nanoTime();
                if (estado == Estado.SIN_VERIFICAR && !avisado && milisegundos(ahora - inicio) > PLAZO_SONDA_MS) {
                    avisado = true;
                    log.error("La escucha de tiempo real no recibió su propia sonda: los eventos no llegarán de otras "
                            + "instancias. Revise que la conexión no pase por un agrupador en modo transacción.");
                }
                if (milisegundos(ahora - ultimoLatido) >= LATIDO_MS) {
                    try (Statement sentencia = abierta.createStatement()) {
                        sentencia.execute("SELECT 1");
                    }
                    ultimoLatido = ahora;
                }
            }
        } finally {
            conexion = null;
        }
    }

    private Connection abrir() throws Exception {
        Properties conexionPropiedades = new Properties();
        conexionPropiedades.putAll(propiedades);
        conexionPropiedades.setProperty("user", usuario);
        if (clave != null) {
            conexionPropiedades.setProperty("password", clave);
        }
        conexionPropiedades.setProperty("tcpKeepAlive", "true");
        conexionPropiedades.setProperty("connectTimeout", "10");
        conexionPropiedades.setProperty("ApplicationName", nombreDeAplicacion());
        return DriverManager.getConnection(url, conexionPropiedades);
    }

    private void enviarSonda(Connection abierta) throws Exception {
        try (PreparedStatement sentencia = abierta.prepareStatement("SELECT pg_notify(?, ?)")) {
            sentencia.setString(1, canal);
            sentencia.setString(2, SobreTiempoReal.sonda(origen));
            sentencia.execute();
        }
    }

    private void procesar(String carga) {
        Optional<String> sonda = SobreTiempoReal.leerSonda(carga);
        if (sonda.isPresent()) {
            if (sonda.get().equals(origen) && estado != Estado.ESCUCHANDO) {
                estado = Estado.ESCUCHANDO;
                log.info("Escucha de tiempo real verificada");
            }
            return;
        }
        Optional<SobreTiempoReal> sobre = SobreTiempoReal.leerEvento(carga);
        if (sobre.isEmpty() || origen.equals(sobre.get().origen())) {
            return;
        }
        try {
            entrega.accept(sobre.get());
        } catch (Exception e) {
            log.warn("No se pudo entregar un evento de tiempo real de otra instancia: {}", e.getClass().getSimpleName());
        }
    }

    private boolean dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return activa;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private static long milisegundos(long nanosegundos) {
        return TimeUnit.NANOSECONDS.toMillis(nanosegundos);
    }
}
