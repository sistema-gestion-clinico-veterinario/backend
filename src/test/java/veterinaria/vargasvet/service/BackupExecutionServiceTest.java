package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.BackupExecution;
import veterinaria.vargasvet.domain.enums.*;
import veterinaria.vargasvet.dto.request.BackupExecutionEventRequest;
import veterinaria.vargasvet.repository.BackupExecutionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BackupExecutionServiceTest {

    @Test
    void registraUnInicioSinGuardarSecretosDeInfraestructura() {
        BackupExecutionRepository repository = mock(BackupExecutionRepository.class);
        when(repository.findByRunIdAndRunAttempt("123", 1)).thenReturn(Optional.empty());
        BackupExecutionService service = new BackupExecutionService(repository);
        Instant inicio = Instant.parse("2026-10-18T07:17:00Z");

        service.record(request(BackupEventPhase.STARTED, BackupRunStatus.IN_PROGRESS, inicio, null));

        var captor = org.mockito.ArgumentCaptor.forClass(BackupExecution.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRecoveryPointId()).isNotNull();
        assertThat(captor.getValue().getDatabaseStatus()).isEqualTo(BackupComponentStatus.PENDING);
        assertThat(captor.getValue().getWorkflowUrl()).startsWith("https://github.com/");
    }

    @Test
    void unEventoDeInicioTardioNoReabreUnaEjecucionFinalizada() {
        BackupExecution finalizada = new BackupExecution();
        finalizada.setId(1L);
        finalizada.setStatus(BackupRunStatus.SUCCESS);
        BackupExecutionRepository repository = mock(BackupExecutionRepository.class);
        when(repository.findByRunIdAndRunAttempt("123", 1)).thenReturn(Optional.of(finalizada));

        new BackupExecutionService(repository).record(request(
                BackupEventPhase.STARTED, BackupRunStatus.IN_PROGRESS,
                Instant.parse("2026-10-18T07:17:00Z"), null));

        verify(repository, never()).save(any());
    }

    @Test
    void unReenvioNoPuedeAlterarUnaEjecucionFinalizada() {
        BackupExecution finalizada = new BackupExecution();
        finalizada.setId(1L);
        finalizada.setStatus(BackupRunStatus.SUCCESS);
        BackupExecutionRepository repository = mock(BackupExecutionRepository.class);
        when(repository.findByRunIdAndRunAttempt("123", 1)).thenReturn(Optional.of(finalizada));

        new BackupExecutionService(repository).record(request(
                BackupEventPhase.COMPLETED, BackupRunStatus.FAILED,
                Instant.parse("2026-10-18T07:17:00Z"), Instant.parse("2026-10-18T07:21:00Z")));

        assertThat(finalizada.getStatus()).isEqualTo(BackupRunStatus.SUCCESS);
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnResultadoFinalSinFechaDeFinalizacion() {
        BackupExecutionRepository repository = mock(BackupExecutionRepository.class);
        when(repository.findByRunIdAndRunAttempt("123", 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new BackupExecutionService(repository).record(request(
                BackupEventPhase.COMPLETED, BackupRunStatus.SUCCESS,
                Instant.parse("2026-10-18T07:17:00Z"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finishedAt");
    }

    private BackupExecutionEventRequest request(BackupEventPhase phase, BackupRunStatus status,
                                                Instant startedAt, Instant finishedAt) {
        return new BackupExecutionEventRequest(
                "123", 1, UUID.fromString("01fa5bc2-2188-42cd-9d7f-631dae309327"), "p01",
                BackupTriggerType.SCHEDULED, phase, status,
                phase == BackupEventPhase.STARTED ? BackupComponentStatus.PENDING : BackupComponentStatus.SUCCESS,
                phase == BackupEventPhase.STARTED ? BackupComponentStatus.PENDING : BackupComponentStatus.SUCCESS,
                phase == BackupEventPhase.STARTED ? BackupComponentStatus.PENDING : BackupComponentStatus.SUCCESS,
                startedAt, finishedAt, "c423a72000000000000000000000000000000000",
                "https://github.com/softvet/backend/actions/runs/123");
    }
}
