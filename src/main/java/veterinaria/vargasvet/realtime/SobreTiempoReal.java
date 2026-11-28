package veterinaria.vargasvet.realtime;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public record SobreTiempoReal(String origen, String destino, byte[] cuerpo) {

    private static final char SEPARADOR = '\n';
    private static final String PREFIJO_EVENTO = "E" + SEPARADOR;
    private static final String PREFIJO_SONDA = "S" + SEPARADOR;
    private static final String PREFIJO_DESTINO = "/topic/";

    public static String evento(String origen, String destino, byte[] cuerpo) {
        if (origen == null || origen.isBlank() || origen.indexOf(SEPARADOR) >= 0) {
            throw new IllegalArgumentException("Origen no válido");
        }
        if (destino == null || !destino.startsWith(PREFIJO_DESTINO) || destino.indexOf(SEPARADOR) >= 0) {
            throw new IllegalArgumentException("Destino no válido");
        }
        return PREFIJO_EVENTO + origen + SEPARADOR + destino + SEPARADOR + new String(cuerpo, StandardCharsets.UTF_8);
    }

    public static String sonda(String origen) {
        if (origen == null || origen.isBlank() || origen.indexOf(SEPARADOR) >= 0) {
            throw new IllegalArgumentException("Origen no válido");
        }
        return PREFIJO_SONDA + origen;
    }

    public static Optional<SobreTiempoReal> leerEvento(String texto) {
        if (texto == null || !texto.startsWith(PREFIJO_EVENTO)) {
            return Optional.empty();
        }
        int finOrigen = texto.indexOf(SEPARADOR, PREFIJO_EVENTO.length());
        if (finOrigen < 0) {
            return Optional.empty();
        }
        int finDestino = texto.indexOf(SEPARADOR, finOrigen + 1);
        if (finDestino < 0) {
            return Optional.empty();
        }
        String origen = texto.substring(PREFIJO_EVENTO.length(), finOrigen);
        String destino = texto.substring(finOrigen + 1, finDestino);
        if (origen.isBlank() || !destino.startsWith(PREFIJO_DESTINO)) {
            return Optional.empty();
        }
        byte[] cuerpo = texto.substring(finDestino + 1).getBytes(StandardCharsets.UTF_8);
        return Optional.of(new SobreTiempoReal(origen, destino, cuerpo));
    }

    public static Optional<String> leerSonda(String texto) {
        if (texto == null || !texto.startsWith(PREFIJO_SONDA) || texto.length() == PREFIJO_SONDA.length()) {
            return Optional.empty();
        }
        return Optional.of(texto.substring(PREFIJO_SONDA.length()));
    }
}
