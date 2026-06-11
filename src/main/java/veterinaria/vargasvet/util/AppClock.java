package veterinaria.vargasvet.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class AppClock {
    public static final ZoneId ZONE_ID = ZoneId.of("America/Lima");
    private static final Clock CLOCK = Clock.system(ZONE_ID);

    private AppClock() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    public static LocalTime currentTime() {
        return LocalTime.now(ZONE_ID);
    }

    public static Instant instantNow() {
        return Instant.now(CLOCK);
    }
}