package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportesClinicosServiceImplTest {

    @Mock CitaRepository citaRepository;
    @Mock RegistroVacunaRepository registroVacunaRepository;
    @Mock RegistroDesparasitacionRepository registroDesparasitacionRepository;
    @Mock ControlPreventivoRepository controlPreventivoRepository;

    private ReportesClinicosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportesClinicosServiceImpl(
                citaRepository,
                registroVacunaRepository,
                registroDesparasitacionRepository,
                controlPreventivoRepository);
    }

    @Test
    void generaReporteSinInvocarHashCodeRecursivoDeMascota() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        Mascota mascota = new Mascota();
        mascota.setId(10L);
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setFechaNacimiento(LocalDate.of(2022, 1, 1));

        Apoderado apoderado = new Apoderado();
        apoderado.setId(20L);
        apoderado.setMascotas(List.of(mascota));
        mascota.setApoderado(apoderado);

        Cita primera = cita(1L, mascota, LocalDateTime.of(2026, 7, 10, 9, 0));
        Cita segunda = cita(2L, mascota, LocalDateTime.of(2026, 7, 20, 10, 0));

        when(citaRepository.findForClinicalReport(
                eq(1), any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
                .thenReturn(List.of(primera, segunda), List.of());
        when(registroVacunaRepository.findProximasVacunas(eq(1), any(), any())).thenReturn(List.of());
        when(registroDesparasitacionRepository.findProximasDesparasitaciones(eq(1), any(), any()))
                .thenReturn(List.of());
        when(controlPreventivoRepository.findProximosByCompany(eq(1), any(), any(), anyCollection()))
                .thenReturn(List.of());

        ReportesClinicosDTO reporte = assertDoesNotThrow(
                () -> service.obtenerReportes(1, desde, hasta, null, null));

        assertThat(reporte.getResumen().getConsultas()).isEqualTo(2);
        assertThat(reporte.getPacientesPorEspecie())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getLabel()).isEqualTo("Perro");
                    assertThat(item.getCount()).isEqualTo(1);
                });
    }

    private Cita cita(Long id, Mascota mascota, LocalDateTime fecha) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setMascota(mascota);
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setFechaHoraInicio(fecha);
        cita.setFechaHoraFin(fecha.plusMinutes(30));
        cita.setTotalServicio(BigDecimal.valueOf(50));
        cita.setMontoPagado(BigDecimal.ZERO);
        return cita;
    }
}
