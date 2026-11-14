package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.dto.request.BulkScheduleRequest;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.service.EmpleadoService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmpleadoControllerHorariosAuditoriaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Mock private EmpleadoService empleadoService;
    @Mock private CitaService citaService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private EmpleadoController controller;

    @Test
    void asignarHorarioMasivoLoAuditaSoloElServicio() {
        BulkScheduleRequest solicitud = new BulkScheduleRequest();

        controller.assignBulkSchedule(10L, solicitud);

        verify(empleadoService).assignBulkSchedule(10L, solicitud);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void eliminarHorarioMasivoLoAuditaSoloElServicio() {
        controller.deleteBulkSchedule(10L, LUNES.toString(), LUNES.plusDays(6).toString(), List.of("LUNES"));

        verify(empleadoService).deleteBulkSchedule(10L, LUNES, LUNES.plusDays(6), List.of("LUNES"));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void clonarUnaSemanaLaAuditaSoloElServicio() {
        controller.cloneWeekSchedule(10L, LUNES.toString(), LUNES.plusDays(7).toString());

        verify(empleadoService).cloneWeekSchedule(10L, LUNES, LUNES.plusDays(7));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void clonarUnDiaLoAuditaSoloElServicio() {
        controller.cloneDaySchedule(10L, LUNES.toString(), LUNES.plusDays(3).toString());

        verify(empleadoService).cloneDaySchedule(10L, LUNES, LUNES.plusDays(3));
        verifyNoInteractions(auditLogService);
    }
}
