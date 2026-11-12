package veterinaria.vargasvet.service.reportes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record RangoDeReporte(LocalDate desde, LocalDate hasta) {

    private static final int MAXIMO_DIAS = 1110;

    public static RangoDeReporte validado(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate inicio = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : hoy;
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > MAXIMO_DIAS) {
            throw new IllegalArgumentException("El rango máximo permitido para reportes es de 36 meses");
        }
        return new RangoDeReporte(inicio, fin);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public RangoDeReporte anterior() {
        LocalDate anteriorHasta = desde.minusDays(1);
        return new RangoDeReporte(anteriorHasta.minusDays(dias() - 1), anteriorHasta);
    }

    public LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    public LocalDateTime finExclusivo() {
        return hasta.plusDays(1).atStartOfDay();
    }
}
