package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.entity.AuditLog;

import java.util.List;

@Data
@Builder
public class DashboardOverviewDTO {
    private DashboardStatsDTO stats;
    private List<AuditLog> recentLogs;
    private List<EmpleadoListResponse> employees;
    private List<CitaResponse> todayAppointments;
    private List<MascotaResponse> pets;
    private List<RolDTO> roles;
    private List<ApoderadoListResponse> guardians;
    private List<EmployeeScheduleReportResponse> schedules;
    private List<PagoListResponse> payments;
    private List<CompanyListResponse> companies;
}
