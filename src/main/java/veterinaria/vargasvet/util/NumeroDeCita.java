package veterinaria.vargasvet.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class NumeroDeCita {

    public static final int LONGITUD_MAXIMA = 24;
    private static final int LONGITUD_DEL_SUFIJO = 10;

    private NumeroDeCita() {
    }

    public static String generar(LocalDate fecha) {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, LONGITUD_DEL_SUFIJO).toUpperCase(Locale.ROOT);
        return "CIT-" + fecha.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + sufijo;
    }
}
