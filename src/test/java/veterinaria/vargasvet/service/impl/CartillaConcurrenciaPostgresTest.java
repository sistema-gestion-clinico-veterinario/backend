package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.BaseDesechableDePruebas;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.mapper.MascotaCartillaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoDesparasitanteRepository;
import veterinaria.vargasvet.repository.TipoVacunaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.AppClock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Registros de la cartilla hechos a la vez por dos hilos contra PostgreSQL real, donde el bloqueo pesimista de la
 * mascota sí existe (H2 no lo emula de forma confiable). Usa su propia base «prueba_cartilla_concurrencia» dentro
 * del servidor local indicado en FLYWAY_TEST_URL, así no choca con las pruebas de migraciones.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "FLYWAY_TEST_URL", matches = ".+")
class CartillaConcurrenciaPostgresTest {

    private static final String BASE_PROPIA = "prueba_cartilla_concurrencia";

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry registro) throws Exception {
        String urlIndicada = System.getenv("FLYWAY_TEST_URL");
        String usuario = System.getenv().getOrDefault("FLYWAY_TEST_USER", "postgres");
        String clave = System.getenv().getOrDefault("FLYWAY_TEST_PASSWORD", "");
        BaseDesechableDePruebas.exigir(urlIndicada);
        String urlPropia = urlIndicada.replaceFirst("/[^/?]+(\\?|$)", "/" + BASE_PROPIA + "$1");
        BaseDesechableDePruebas.exigir(urlPropia);
        try (Connection conexion = DriverManager.getConnection(urlIndicada, usuario, clave);
             Statement sentencia = conexion.createStatement();
             ResultSet existente = sentencia.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + BASE_PROPIA + "'")) {
            if (!existente.next()) {
                try (Statement crear = conexion.createStatement()) {
                    crear.execute("CREATE DATABASE " + BASE_PROPIA);
                }
            }
        }
        registro.add("spring.datasource.url", () -> urlPropia);
        registro.add("spring.datasource.username", () -> usuario);
        registro.add("spring.datasource.password", () -> clave);
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registro.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registro.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registro.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;
    @Autowired private TipoVacunaRepository tipoVacunaRepository;
    @Autowired private TipoDesparasitanteRepository tipoDesparasitanteRepository;
    @Autowired private RegistroVacunaRepository vacunaRepository;
    @Autowired private RegistroDesparasitacionRepository desparasitacionRepository;
    @Autowired private ControlPreventivoRepository controlRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private CartillaServiceImpl servicio() {
        return new CartillaServiceImpl(
                mascotaRepository, historiaClinicaRepository, empleadoRepository, serviciosRepository,
                tipoVacunaRepository, tipoDesparasitanteRepository, vacunaRepository, desparasitacionRepository,
                controlRepository, citaRepository, mock(UsuarioContactoService.class), mock(AuditLogService.class),
                mock(SimpMessagingTemplate.class), mock(MascotaCartillaMapper.class),
                mock(veterinaria.vargasvet.service.PetOwnershipService.class));
    }

    private EscenarioCartillaParaPruebas.Datos crearEscenario(TransactionTemplate transaccion) {
        return transaccion.execute(estado -> new EscenarioCartillaParaPruebas(companyRepository, usuarioRepository,
                apoderadoRepository, mascotaRepository, empleadoRepository, serviciosRepository, tipoVacunaRepository).crear());
    }

    private CartillaAplicacionRequest peticion(EscenarioCartillaParaPruebas.Datos datos, LocalDate fecha, int intervaloDias) {
        CartillaAplicacionRequest request = new CartillaAplicacionRequest();
        request.setMascotaId(datos.mascotaId());
        request.setServicioId(datos.servicioId());
        request.setTipoVacunaId(datos.vacunaId());
        request.setFechaAplicacion(fecha);
        request.setIntervaloCantidad(intervaloDias);
        request.setIntervaloUnidad(IntervaloUnidad.DIAS);
        return request;
    }

    /** Lanza las peticiones a la vez, una por hilo, y devuelve el error de cada una (null si tuvo éxito). */
    private List<Throwable> registrarALaVez(CartillaServiceImpl servicio, TransactionTemplate transaccion,
                                            Integer vetUserId, List<CartillaAplicacionRequest> peticiones) throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(peticiones.size());
        try {
            List<Future<Throwable>> resultados = peticiones.stream().map(request -> hilos.submit(() -> {
                try {
                    EscenarioCartillaParaPruebas.autenticarVeterinario(vetUserId);
                    inicio.await();
                    transaccion.executeWithoutResult(estado -> servicio.registrarVacunacion(request));
                    return (Throwable) null;
                } catch (Throwable error) {
                    return error;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            inicio.countDown();
            java.util.ArrayList<Throwable> errores = new java.util.ArrayList<>();
            for (Future<Throwable> resultado : resultados) {
                errores.add(resultado.get(60, TimeUnit.SECONDS));
            }
            return errores;
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos registros idénticos a la vez (doble clic) crean una sola vacuna y un solo cobro; el otro se rechaza")
    void dosRegistrosIdenticosSimultaneosCreanUnoSoloYRechazanAlOtro() throws Exception {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        EscenarioCartillaParaPruebas.Datos datos = crearEscenario(transaccion);
        CartillaAplicacionRequest request = peticion(datos, AppClock.today(), 21);

        List<Throwable> errores = registrarALaVez(servicio(), transaccion, datos.vetUserId(), List.of(request, request));

        assertThat(errores.stream().filter(error -> error == null).count()).as("registros que tuvieron éxito").isEqualTo(1);
        Throwable rechazo = errores.stream().filter(error -> error != null).findFirst().orElseThrow();
        assertThat(rechazo).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("misma fecha de aplicacion");
        assertThat(vacunaRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(datos.mascotaId())).hasSize(1);
        assertThat(citaRepository.findAll().stream()
                .filter(cita -> cita.getMascota().getId().equals(datos.mascotaId())).count()).as("cuentas enviadas a Caja").isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos registros de fechas distintas a la vez no duplican el próximo control que comparten")
    void registrosConcurrentesDeFechasDistintasConservanUnSoloProximoControl() throws Exception {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        EscenarioCartillaParaPruebas.Datos datos = crearEscenario(transaccion);
        LocalDate hoy = AppClock.today();
        List<CartillaAplicacionRequest> peticiones = List.of(
                peticion(datos, hoy, 21),
                peticion(datos, hoy.minusDays(1), 22));

        List<Throwable> errores = registrarALaVez(servicio(), transaccion, datos.vetUserId(), peticiones);

        assertThat(errores).as("ambos registros son válidos").containsOnlyNulls();
        assertThat(vacunaRepository.findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(datos.mascotaId())).hasSize(2);
        List<ControlPreventivo> proximos = transaccion.execute(estado ->
                controlRepository.findByMascotaIdOrderByFechaRecomendadaDesc(datos.mascotaId()).stream()
                        .filter(control -> control.getTipo() == TipoControlPreventivo.VACUNACION)
                        .filter(control -> control.getFechaRecomendada().equals(hoy.plusDays(21)))
                        .toList());
        assertThat(proximos).hasSize(1);
    }
}
