package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioPreventivoServiceImplTest {
    @Mock ControlPreventivoRepository controlRepository;
    @Mock RecordatorioPreventivoRepository recordatorioRepository;
    @Mock EmailService emailService;
    @Mock veterinaria.vargasvet.service.ConsentimientoDatosService consentimientoDatosService;

    private RecordatorioPreventivoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecordatorioPreventivoServiceImpl(controlRepository, recordatorioRepository, emailService,
                new veterinaria.vargasvet.service.OwnerContactPolicy(mock(UsuarioContactoService.class)),
                new veterinaria.vargasvet.service.PetOwnershipService(
                        mock(veterinaria.vargasvet.repository.MascotaRepository.class),
                        mock(veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository.class),
                        mock(veterinaria.vargasvet.repository.CitaRepository.class),
                        mock(veterinaria.vargasvet.service.AuditLogService.class)),
                consentimientoDatosService);
        lenient().when(consentimientoDatosService.usuariosQueAutorizaron(anyCollection(), anyInt(), any()))
                .thenAnswer(invocation -> Set.copyOf(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("No envía el recordatorio a un correo que la persona aún no verificó")
    void noEnviaAUnCorreoSinVerificar() {
        ControlPreventivo vacuna = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        vacuna.getMascota().getApoderado().getUser().setEmailVerified(false);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(vacuna));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        when(recordatorioRepository.findApoderadoIdsWithRecentReminder(anyCollection(), any())).thenReturn(List.of());

        service.procesarRecordatorios();

        verifyNoInteractions(emailService);
        verify(recordatorioRepository, never()).save(any(RecordatorioPreventivo.class));
    }

    @Test
    @DisplayName("No envía recordatorios sin una autorización vigente")
    void noEnviaSinAutorizacionVigente() {
        ControlPreventivo vacuna = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(vacuna));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        when(consentimientoDatosService.usuariosQueAutorizaron(anyCollection(), eq(7), any()))
                .thenReturn(Set.of());

        service.procesarRecordatorios();

        verifyNoInteractions(emailService);
        verify(recordatorioRepository, never()).save(any(RecordatorioPreventivo.class));
    }

    @Test
    @DisplayName("[CP-RF33-03] Agrupa vacuna y desparasitación en un correo preventivo")
    void agrupaControlesDelMismoPropietarioEnUnSoloCorreo() {
        ControlPreventivo vacuna = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        ControlPreventivo desparasitacion = control(2L, "Desparasitacion", TipoControlPreventivo.DESPARASITACION, 1L);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(vacuna, desparasitacion));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        when(recordatorioRepository.findApoderadoIdsWithRecentReminder(anyCollection(), any())).thenReturn(List.of());
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
        when(emailService.sendEmail(any(Mail.class), eq("email/recordatorio-preventivo-template")))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));

        service.procesarRecordatorios();

        verify(emailService, times(1)).sendEmail(any(Mail.class), eq("email/recordatorio-preventivo-template"));
        verify(recordatorioRepository, times(2)).save(any(RecordatorioPreventivo.class));
    }

    @Test
    @DisplayName("Si el envío del correo falla, no se marca el recordatorio como enviado (para poder reintentar al día siguiente)")
    void noGuardaElRecordatorioSiElCorreoFallaAlEnviarse() {
        ControlPreventivo vacuna = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(vacuna));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        when(recordatorioRepository.findApoderadoIdsWithRecentReminder(anyCollection(), any())).thenReturn(List.of());
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
        when(emailService.sendEmail(any(Mail.class), eq("email/recordatorio-preventivo-template")))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(false));

        service.procesarRecordatorios();

        verify(emailService, times(1)).sendEmail(any(Mail.class), eq("email/recordatorio-preventivo-template"));
        verify(recordatorioRepository, never()).save(any(RecordatorioPreventivo.class));
    }

    @Test
    @DisplayName("[CP-RF33-04] Evita recordatorios dentro del intervalo mínimo de siete días")
    void respetaElIntervaloMinimoDeSieteDiasPorPropietario() {
        when(controlRepository.findReminderCandidates(any(), any()))
                .thenReturn(List.of(control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L)));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of());
        when(recordatorioRepository.findApoderadoIdsWithRecentReminder(anyCollection(), any())).thenReturn(List.of(1L));

        service.procesarRecordatorios();

        verifyNoInteractions(emailService);
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("[CP-RF33-04] No duplica un recordatorio del mismo control, aviso y fecha")
    void noDuplicaLaMismaClaveDeRecordatorio() {
        ControlPreventivo control = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        RecordatorioPreventivoRepository.ReminderKey existente =
                mock(RecordatorioPreventivoRepository.ReminderKey.class);
        when(existente.getControlId()).thenReturn(1L);
        when(existente.getTipoAviso()).thenReturn(TipoAvisoRecordatorio.PROXIMO);
        when(existente.getFechaProgramada()).thenReturn(control.getFechaRecomendada());
        when(existente.getApoderadoId()).thenReturn(1L);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(control));
        when(recordatorioRepository.findExistingKeys(anyCollection())).thenReturn(List.of(existente));

        service.procesarRecordatorios();

        verifyNoInteractions(emailService);
        verify(recordatorioRepository, never()).save(any());
    }

    private ControlPreventivo control(Long id, String nombre, TipoControlPreventivo tipo, Long apoderadoId) {
        Company company = new Company();
        company.setId(7);
        company.setName("Patitas Felices");
        Usuario user = new Usuario();
        user.setNombre("Ana");
        user.setApellido("Perez");
        user.setEmail("ana@example.com");
        user.setEmailVerified(true);
        user.setActivo(true);
        user.setId(Math.toIntExact(apoderadoId));
        user.setCompany(company);
        Apoderado apoderado = new Apoderado();
        apoderado.setCompany(company);
        apoderado.setId(apoderadoId);
        apoderado.setUser(user);
        Mascota mascota = new Mascota();
        mascota.setId(id);
        mascota.setNombreCompleto("Mascota " + id);
        mascota.setApoderado(apoderado);
        ControlPreventivo control = new ControlPreventivo();
        control.setId(id);
        control.setMascota(mascota);
        control.setNombreControl(nombre);
        control.setTipo(tipo);
        control.setEstado(EstadoControlPreventivo.PROXIMO);
        control.setFechaRecomendada(AppClock.today().plusDays(3));
        return control;
    }
}
