package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.BackupExecution;
import veterinaria.vargasvet.domain.enums.BackupRunStatus;
import veterinaria.vargasvet.repository.BackupExecutionRepository;
import veterinaria.vargasvet.security.BackupReportTokenVerifier;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupSummaryServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T15:00:00Z");

    private final BackupExecutionRepository repository = mock(BackupExecutionRepository.class);
    private final BackupSummaryService service =
            new BackupSummaryService(repository, new BackupReportTokenVerifier("x".repeat(32)));

    private BackupExecution ejecucion(BackupRunStatus estado, String inicio, String fin) {
        BackupExecution e = new BackupExecution();
        e.setStatus(estado);
        e.setStartedAt(Instant.parse(inicio));
        e.setFinishedAt(fin == null ? null : Instant.parse(fin));
        return e;
    }

    private void conUltima(BackupExecution ultima) {
        when(repository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.ofNullable(ultima));
        when(repository.findFirstByStatusOrderByStartedAtDesc(any())).thenReturn(Optional.empty());
    }

    private void conExito(BackupExecution exito) {
        when(repository.findFirstByStatusOrderByStartedAtDesc(BackupRunStatus.SUCCESS)).thenReturn(Optional.of(exito));
    }

    @Test
    void sinNingunReporteElEstadoEsSinDatosYSeIndicaSiLaIntegracionEstaConfigurada() {
        conUltima(null);

        var resumen = service.summary(AHORA);

        assertThat(resumen.health()).isEqualTo("SIN_DATOS");
        assertThat(resumen.reportingConfigured()).isTrue();
        assertThat(resumen.lastSuccessAt()).isNull();
        assertThat(new BackupSummaryService(repository, new BackupReportTokenVerifier("corto")).summary(AHORA)
                .reportingConfigured()).isFalse();
    }

    @Test
    void unRespaldoExitosoRecienteEstaAlDia() {
        BackupExecution exito = ejecucion(BackupRunStatus.SUCCESS, "2026-10-18T07:17:00Z", "2026-10-18T07:20:00Z");
        conUltima(exito);
        conExito(exito);

        var resumen = service.summary(AHORA);

        assertThat(resumen.health()).isEqualTo("AL_DIA");
        assertThat(resumen.lastSuccessAt()).isEqualTo(Instant.parse("2026-10-18T07:20:00Z"));
    }

    @Test
    void sinExitoDesdeHaceMasDe36HorasPideAtencionYDe72EsCritico() {
        BackupExecution viejo = ejecucion(BackupRunStatus.SUCCESS, "2026-10-16T07:17:00Z", "2026-10-16T07:20:00Z");
        conUltima(viejo);
        conExito(viejo);
        assertThat(service.summary(AHORA).health()).isEqualTo("ATENCION");

        BackupExecution muyViejo = ejecucion(BackupRunStatus.SUCCESS, "2026-10-14T07:17:00Z", "2026-10-14T07:20:00Z");
        conUltima(muyViejo);
        conExito(muyViejo);
        assertThat(service.summary(AHORA).health()).isEqualTo("CRITICO");
    }

    @Test
    void sinNingunExitoRegistradoEsCritico() {
        conUltima(ejecucion(BackupRunStatus.FAILED, "2026-10-18T07:17:00Z", "2026-10-18T07:19:00Z"));

        assertThat(service.summary(AHORA).health()).isEqualTo("CRITICO");
    }

    @Test
    void siLaUltimaEjecucionFalloAunqueHayaUnExitoRecienteSePideAtencion() {
        conUltima(ejecucion(BackupRunStatus.FAILED, "2026-10-18T07:17:00Z", "2026-10-18T07:19:00Z"));
        conExito(ejecucion(BackupRunStatus.SUCCESS, "2026-10-17T07:17:00Z", "2026-10-17T07:20:00Z"));

        assertThat(service.summary(AHORA).health()).isEqualTo("ATENCION");
    }

    @Test
    void unaEjecucionEnCursoNoCuentaComoProblema() {
        BackupExecution exito = ejecucion(BackupRunStatus.SUCCESS, "2026-10-17T07:17:00Z", "2026-10-17T07:20:00Z");
        conUltima(ejecucion(BackupRunStatus.IN_PROGRESS, "2026-10-18T14:55:00Z", null));
        conExito(exito);

        assertThat(service.summary(AHORA).health()).isEqualTo("AL_DIA");
    }

    @Test
    void cuentaLosExitosYLosProblemasDeLosUltimos30Dias() {
        conUltima(ejecucion(BackupRunStatus.SUCCESS, "2026-10-18T07:17:00Z", "2026-10-18T07:20:00Z"));
        when(repository.countByStatusAndStartedAtAfter(eq(BackupRunStatus.SUCCESS), any())).thenReturn(27L);
        when(repository.countByStatusAndStartedAtAfter(eq(BackupRunStatus.FAILED), any())).thenReturn(2L);
        when(repository.countByStatusAndStartedAtAfter(eq(BackupRunStatus.PARTIAL), any())).thenReturn(1L);

        var resumen = service.summary(AHORA);

        assertThat(resumen.successCount30Days()).isEqualTo(27);
        assertThat(resumen.problemCount30Days()).isEqualTo(3);
    }

    @Test
    void laProximaEjecucionEsLaSiguienteOcurrenciaDeLas0717Utc() {
        assertThat(BackupSummaryService.nextScheduledAfter(Instant.parse("2026-10-18T05:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-18T07:17:00Z"));
        assertThat(BackupSummaryService.nextScheduledAfter(Instant.parse("2026-10-18T15:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-19T07:17:00Z"));
        assertThat(BackupSummaryService.nextScheduledAfter(Instant.parse("2026-10-18T07:17:00Z")))
                .isEqualTo(Instant.parse("2026-10-19T07:17:00Z"));
    }
}
