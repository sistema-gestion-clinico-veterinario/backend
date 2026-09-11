package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ReportesClinicosServiceImplTest {

    @Mock CitaRepository citaRepository;
    @Mock RegistroVacunaRepository registroVacunaRepository;
    @Mock RegistroDesparasitacionRepository registroDesparasitacionRepository;
    @Mock ControlPreventivoRepository controlPreventivoRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock AccesoValidator accesoValidator;

    private ReportesClinicosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportesClinicosServiceImpl(
                citaRepository,
                registroVacunaRepository,
                registroDesparasitacionRepository,
                controlPreventivoRepository,
                empleadoRepository,
                purchaseRepository,
                accesoValidator);
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
        when(registroVacunaRepository.findProximasVacunas(eq(1), any(), any(), any())).thenReturn(List.of());
        when(registroDesparasitacionRepository.findProximasDesparasitaciones(eq(1), any(), any(), any()))
                .thenReturn(List.of());
        when(controlPreventivoRepository.findProximosByCompany(eq(1), any(), any(), anyCollection(), any()))
                .thenReturn(List.of());

        ReportesClinicosDTO reporte;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            reporte = assertDoesNotThrow(
                    () -> service.obtenerReportes(1, desde, hasta, null, null));
        }

        assertThat(reporte.getResumen().getConsultas()).isEqualTo(2);
        assertThat(reporte.getPacientesPorEspecie())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getLabel()).isEqualTo("Perro");
                    assertThat(item.getCount()).isEqualTo(1);
                });
    }

    @Test
    void unRolConAlcanceOwnNoPuedeVerElReporteDeOtroEmpleado() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        veterinaria.vargasvet.domain.entity.Empleado propioEmpleado =
                new veterinaria.vargasvet.domain.entity.Empleado();
        propioEmpleado.setId(55L);

        when(empleadoRepository.findByUserId(7)).thenReturn(java.util.Optional.of(propioEmpleado));
        org.mockito.Mockito.lenient().when(accesoValidator.can(anyString(), anyString())).thenReturn(true);
        when(citaRepository.findForClinicalReport(
                eq(1), any(LocalDateTime.class), any(LocalDateTime.class), eq(55L), isNull()))
                .thenReturn(List.of(), List.of());
        when(registroVacunaRepository.findProximasVacunas(eq(1), any(), any(), any())).thenReturn(List.of());
        when(registroDesparasitacionRepository.findProximasDesparasitaciones(eq(1), any(), any(), any()))
                .thenReturn(List.of());
        when(controlPreventivoRepository.findProximosByCompany(eq(1), any(), any(), anyCollection(), any()))
                .thenReturn(List.of());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(1);
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7);
            when(accesoValidator.canAccessCompanyData("VISTA_REPORTES")).thenReturn(false);

            // Solicita explícitamente el reporte de OTRO empleado (999); debe ser ignorado.
            assertDoesNotThrow(() -> service.obtenerReportes(1, desde, hasta, 999L, null));
        }

        // Verifica que la consulta real a la BD se hizo con el id del propio empleado (55),
        // nunca con el 999 solicitado por query param.
        org.mockito.Mockito.verify(citaRepository, org.mockito.Mockito.atLeastOnce())
                .findForClinicalReport(eq(1), any(LocalDateTime.class), any(LocalDateTime.class), eq(55L), isNull());
        org.mockito.Mockito.verify(citaRepository, org.mockito.Mockito.never())
                .findForClinicalReport(eq(1), any(LocalDateTime.class), any(LocalDateTime.class), eq(999L), isNull());
    }

    @Test
    void unEmpleadoQueSoloTienePermisoDeCitasNoVeVacunacionNiIngresos() {
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

        Cita cita = cita(1L, mascota, LocalDateTime.of(2026, 7, 10, 9, 0));

        when(citaRepository.findForClinicalReport(
                eq(1), any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
                .thenReturn(List.of(cita), List.of());

        ReportesClinicosDTO reporte;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(1);
            when(accesoValidator.canAccessCompanyData("VISTA_REPORTES")).thenReturn(true);
            when(accesoValidator.can("VISTA_CITAS", "LEER")).thenReturn(true);
            when(accesoValidator.can("VISTA_PAGOS", "LEER")).thenReturn(false);
            when(accesoValidator.can("VISTA_MASCOTAS", "LEER")).thenReturn(false);
            when(accesoValidator.can("VISTA_CONTROL_PREVENTIVO", "LEER")).thenReturn(false);

            reporte = assertDoesNotThrow(() -> service.obtenerReportes(1, desde, hasta, null, null));
        }

        // Tiene VISTA_CITAS: las secciones basadas en citas sí se calculan (no son null).
        assertThat(reporte.getConsultasPorEstado()).isNotNull();
        assertThat(reporte.getConsultasPorMes()).isNotNull();
        assertThat(reporte.getConsultasPorVeterinario()).isNotNull();
        assertThat(reporte.getServiciosMasSolicitados()).isNotNull();
        assertThat(reporte.getFrecuenciaConsultasPorPaciente()).isNotNull();
        assertThat(reporte.getDemandaPorHorario()).isNotNull();

        // No tiene VISTA_CONTROL_PREVENTIVO: nada de vacunación/desparasitación/controles.
        assertThat(reporte.getProximasVacunas()).isNull();
        assertThat(reporte.getProximasDesparasitaciones()).isNull();
        assertThat(reporte.getControlesPreventivosProximos()).isNull();

        // No tiene VISTA_PAGOS: los ingresos (dato financiero) no se exponen.
        assertThat(reporte.getResumen().getIngresos()).isNull();
        assertThat(reporte.getResumenAnterior().getIngresos()).isNull();

        // No tiene VISTA_MASCOTAS: nada de composición de pacientes.
        assertThat(reporte.getPacientesPorEspecie()).isNull();
        assertThat(reporte.getPacientesPorRangoEdad()).isNull();
    }

    @Test
    void losIngresosExcluyenCanceladasYNoAsistioYSeCapanAlTotalDelServicio() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        Mascota mascota = new Mascota();
        mascota.setId(10L);
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(20L);
        apoderado.setMascotas(List.of(mascota));
        mascota.setApoderado(apoderado);

        Cita completada = cita(1L, mascota, LocalDateTime.of(2026, 7, 5, 9, 0));
        completada.setEstado(EstadoCita.COMPLETADA);
        completada.setTotalServicio(BigDecimal.valueOf(100));
        completada.setMontoPagado(BigDecimal.valueOf(100));

        Cita cancelada = cita(2L, mascota, LocalDateTime.of(2026, 7, 10, 9, 0));
        cancelada.setEstado(EstadoCita.CANCELADA);
        cancelada.setTotalServicio(BigDecimal.valueOf(200));
        cancelada.setMontoPagado(BigDecimal.valueOf(200));

        Cita noAsistio = cita(3L, mascota, LocalDateTime.of(2026, 7, 15, 9, 0));
        noAsistio.setEstado(EstadoCita.NO_ASISTIO);
        noAsistio.setTotalServicio(BigDecimal.valueOf(150));
        noAsistio.setMontoPagado(BigDecimal.valueOf(150));

        // Un pago registrado por error por encima del precio del servicio no debe inflar los ingresos.
        Cita sobrepagada = cita(4L, mascota, LocalDateTime.of(2026, 7, 20, 9, 0));
        sobrepagada.setEstado(EstadoCita.PROGRAMADA);
        sobrepagada.setTotalServicio(BigDecimal.valueOf(80));
        sobrepagada.setMontoPagado(BigDecimal.valueOf(999));

        when(citaRepository.findForClinicalReport(
                eq(1), any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
                .thenReturn(List.of(completada, cancelada, noAsistio, sobrepagada), List.of());
        when(registroVacunaRepository.findProximasVacunas(eq(1), any(), any(), any())).thenReturn(List.of());
        when(registroDesparasitacionRepository.findProximasDesparasitaciones(eq(1), any(), any(), any()))
                .thenReturn(List.of());
        when(controlPreventivoRepository.findProximosByCompany(eq(1), any(), any(), anyCollection(), any()))
                .thenReturn(List.of());

        ReportesClinicosDTO reporte;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            reporte = assertDoesNotThrow(() -> service.obtenerReportes(1, desde, hasta, null, null));
        }

        // 100 (completada) + 80 (sobrepagada, capada al total del servicio) = 180.
        // Cancelada y no-asistió quedan fuera aunque tengan monto pagado registrado.
        assertThat(reporte.getResumen().getIngresos()).isEqualByComparingTo(BigDecimal.valueOf(180).setScale(2));
    }

    @Test
    void elResumenAnteriorSeCalculaSobreElPeriodoInmediatamenteAnteriorDeIgualDuracion() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        Mascota mascota = new Mascota();
        mascota.setId(10L);
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(20L);
        apoderado.setMascotas(List.of(mascota));
        mascota.setApoderado(apoderado);

        Cita citaActual = cita(1L, mascota, LocalDateTime.of(2026, 7, 10, 9, 0));
        Cita citaAnteriorUno = cita(2L, mascota, LocalDateTime.of(2026, 6, 15, 9, 0));
        Cita citaAnteriorDos = cita(3L, mascota, LocalDateTime.of(2026, 6, 20, 9, 0));

        // El periodo actual (jul) tiene 31 días -> el anterior debe ser exactamente los 31 días
        // inmediatamente previos: 31-may al 30-jun (fin exclusivo = inicio del periodo actual).
        when(citaRepository.findForClinicalReport(
                eq(1),
                eq(LocalDate.of(2026, 7, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 8, 1).atStartOfDay()),
                isNull(), isNull()))
                .thenReturn(List.of(citaActual));
        when(citaRepository.findForClinicalReport(
                eq(1),
                eq(LocalDate.of(2026, 5, 31).atStartOfDay()),
                eq(LocalDate.of(2026, 7, 1).atStartOfDay()),
                isNull(), isNull()))
                .thenReturn(List.of(citaAnteriorUno, citaAnteriorDos));
        when(registroVacunaRepository.findProximasVacunas(eq(1), any(), any(), any())).thenReturn(List.of());
        when(registroDesparasitacionRepository.findProximasDesparasitaciones(eq(1), any(), any(), any()))
                .thenReturn(List.of());
        when(controlPreventivoRepository.findProximosByCompany(eq(1), any(), any(), anyCollection(), any()))
                .thenReturn(List.of());

        ReportesClinicosDTO reporte;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            reporte = assertDoesNotThrow(() -> service.obtenerReportes(1, desde, hasta, null, null));
        }

        assertThat(reporte.getResumen().getConsultas()).isEqualTo(1);
        assertThat(reporte.getResumenAnterior().getConsultas()).isEqualTo(2);
    }

    @Test
    void rechazaUnRangoDeFechasMayorA36Meses() {
        LocalDate desde = LocalDate.of(2020, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerReportes(1, desde, hasta, null, null));
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
