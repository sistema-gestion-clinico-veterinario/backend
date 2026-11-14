package veterinaria.vargasvet.domain.enums;

import java.time.DayOfWeek;

public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

    public static DiaSemana desde(DayOfWeek diaDeLaSemana) {
        return switch (diaDeLaSemana) {
            case MONDAY -> LUNES;
            case TUESDAY -> MARTES;
            case WEDNESDAY -> MIERCOLES;
            case THURSDAY -> JUEVES;
            case FRIDAY -> VIERNES;
            case SATURDAY -> SABADO;
            case SUNDAY -> DOMINGO;
        };
    }
}
