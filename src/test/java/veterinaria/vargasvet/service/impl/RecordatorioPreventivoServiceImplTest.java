package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioPreventivoServiceImplTest {
    @Mock ControlPreventivoRepository controlRepository;
    @Mock RecordatorioPreventivoRepository recordatorioRepository;
    @Mock EmailService emailService;

    private RecordatorioPreventivoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecordatorioPreventivoServiceImpl(controlRepository, recordatorioRepository, emailService);
    }

    @Test
    void agrupaControlesDelMismoPropietarioEnUnSoloCorreo() {
        ControlPreventivo vacuna = control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L);
        ControlPreventivo desparasitacion = control(2L, "Desparasitacion", TipoControlPreventivo.DESPARASITACION, 1L);
        when(controlRepository.findReminderCandidates(any(), any())).thenReturn(List.of(vacuna, desparasitacion));
        when(recordatorioRepository.existsByControlPreventivoIdAndTipoAviso(anyLong(), any())).thenReturn(false);
        when(recordatorioRepository.countByApoderadoIdAndFechaEnvioAfter(anyLong(), any())).thenReturn(0L);
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());

        service.procesarRecordatorios();

        verify(emailService, times(1)).sendEmail(any(Mail.class), eq("email/recordatorio-preventivo-template"));
        verify(recordatorioRepository, times(2)).save(any(RecordatorioPreventivo.class));
    }

    @Test
    void respetaElIntervaloMinimoDeSieteDiasPorPropietario() {
        when(controlRepository.findReminderCandidates(any(), any()))
                .thenReturn(List.of(control(1L, "Antirrabica", TipoControlPreventivo.VACUNACION, 1L)));
        when(recordatorioRepository.existsByControlPreventivoIdAndTipoAviso(anyLong(), any())).thenReturn(false);
        when(recordatorioRepository.countByApoderadoIdAndFechaEnvioAfter(anyLong(), any())).thenReturn(1L);

        service.procesarRecordatorios();

        verifyNoInteractions(emailService);
        verify(recordatorioRepository, never()).save(any());
    }

    private ControlPreventivo control(Long id, String nombre, TipoControlPreventivo tipo, Long apoderadoId) {
        Company company = new Company();
        company.setName("Patitas Felices");
        Usuario user = new Usuario();
        user.setNombre("Ana");
        user.setApellido("Perez");
        user.setEmail("ana@example.com");
        user.setCompany(company);
        Apoderado apoderado = new Apoderado();
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
