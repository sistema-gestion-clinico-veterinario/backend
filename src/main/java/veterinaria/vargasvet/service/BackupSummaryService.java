package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.BackupExecution;
import veterinaria.vargasvet.domain.enums.BackupRunStatus;
import veterinaria.vargasvet.dto.response.BackupSummaryResponse;
import veterinaria.vargasvet.repository.BackupExecutionRepository;
import veterinaria.vargasvet.security.BackupReportTokenVerifier;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BackupSummaryService {

    public static final String SIN_DATOS = "SIN_DATOS";
    public static final String AL_DIA = "AL_DIA";
    public static final String ATENCION = "ATENCION";
    public static final String CRITICO = "CRITICO";

    static final int HORAS_SIN_EXITO_PARA_ATENCION = 36;
    static final int HORAS_SIN_EXITO_PARA_CRITICO = 72;
    private static final LocalTime HORA_PROGRAMADA_UTC = LocalTime.of(7, 17);
    private static final int DIAS_DE_RESUMEN = 30;

    private final BackupExecutionRepository repository;
    private final BackupReportTokenVerifier tokenVerifier;

    @Transactional(readOnly = true)
    public BackupSummaryResponse summary() {
        return summary(AppClock.instantNow());
    }

    @Transactional(readOnly = true)
    public BackupSummaryResponse summary(Instant now) {
        Optional<BackupExecution> latest = repository.findFirstByOrderByStartedAtDesc();
        Instant lastSuccess = repository.findFirstByStatusOrderByStartedAtDesc(BackupRunStatus.SUCCESS)
                .map(BackupSummaryService::finishedOrStarted).orElse(null);
        Instant lastFailure = Stream.of(BackupRunStatus.FAILED, BackupRunStatus.PARTIAL)
                .map(repository::findFirstByStatusOrderByStartedAtDesc)
                .flatMap(Optional::stream)
                .map(BackupSummaryService::finishedOrStarted)
                .max(Comparator.naturalOrder()).orElse(null);

        Instant since = now.minus(Duration.ofDays(DIAS_DE_RESUMEN));
        long successes = repository.countByStatusAndStartedAtAfter(BackupRunStatus.SUCCESS, since);
        long problems = repository.countByStatusAndStartedAtAfter(BackupRunStatus.FAILED, since)
                + repository.countByStatusAndStartedAtAfter(BackupRunStatus.PARTIAL, since);

        return new BackupSummaryResponse(
                health(latest.orElse(null), lastSuccess, now),
                tokenVerifier.isConfigured(),
                latest.map(BackupExecution::getStartedAt).orElse(null),
                lastSuccess,
                lastFailure,
                successes,
                problems,
                nextScheduledAfter(now),
                HORAS_SIN_EXITO_PARA_ATENCION);
    }

    static String health(BackupExecution latest, Instant lastSuccess, Instant now) {
        if (latest == null) {
            return SIN_DATOS;
        }
        String health;
        if (lastSuccess == null) {
            health = CRITICO;
        } else {
            long hours = Duration.between(lastSuccess, now).toHours();
            health = hours > HORAS_SIN_EXITO_PARA_CRITICO ? CRITICO
                    : hours > HORAS_SIN_EXITO_PARA_ATENCION ? ATENCION : AL_DIA;
        }
        boolean latestHadProblems = latest.getStatus() == BackupRunStatus.FAILED
                || latest.getStatus() == BackupRunStatus.PARTIAL;
        return latestHadProblems && AL_DIA.equals(health) ? ATENCION : health;
    }

    static Instant nextScheduledAfter(Instant now) {
        ZonedDateTime candidate = now.atZone(ZoneOffset.UTC).with(HORA_PROGRAMADA_UTC);
        if (!candidate.toInstant().isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.toInstant();
    }

    private static Instant finishedOrStarted(BackupExecution execution) {
        return execution.getFinishedAt() != null ? execution.getFinishedAt() : execution.getStartedAt();
    }
}
