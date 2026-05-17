package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.EmpleadoListResponse;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

import java.util.List;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRequest dto);
    UserProfileDTO updateEmpleado(Long empleadoId, EmpleadoRequest dto);
    void cambiarEstado(Long empleadoId, Boolean nuevoEstado);
    void eliminar(Long empleadoId);
    Page<EmpleadoListResponse> listar(Integer companyId, String nombre, String apellido, String email, Long tipoEmpleadoId, Long especialidadId, int page, int size);
    EmpleadoRequest findById(Long id);
    List<HorarioEmpleadoResponse> getHorario(Long empleadoId);
    void assignBulkSchedule(Long empleadoId, veterinaria.vargasvet.dto.request.BulkScheduleRequest request);
    void deleteHorario(Long horarioId);
    void updateHorario(Long horarioId, veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest request);
    void cloneWeekSchedule(Long empleadoId, java.time.LocalDate sourceStartDate, java.time.LocalDate targetStartDate);
    void cloneDaySchedule(Long empleadoId, java.time.LocalDate sourceDate, java.time.LocalDate targetDate);
    List<veterinaria.vargasvet.dto.response.EmployeeScheduleReportResponse> getSchedulesReport(Integer companyId);
}
