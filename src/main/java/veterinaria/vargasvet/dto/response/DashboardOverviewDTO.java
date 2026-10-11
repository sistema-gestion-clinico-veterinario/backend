package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardOverviewDTO {
    private DashboardStatsDTO stats;
    private List<DashboardAuditSummary> recentLogs;
    private List<DashboardEmployeeSummary> employees;
    private List<DashboardAppointmentSummary> todayAppointments;
    private List<DashboardPetSummary> pets;
    private List<RolDTO> roles;
    private List<DashboardGuardianSummary> guardians;
    private List<EmployeeScheduleReportResponse> schedules;
    private List<DashboardPaymentSummary> payments;
    private List<DashboardCompanySummary> companies;
}
