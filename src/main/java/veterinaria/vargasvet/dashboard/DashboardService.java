package veterinaria.vargasvet.dashboard;

import veterinaria.vargasvet.dashboard.DashboardStatsDTO;

public interface DashboardService {
    DashboardStatsDTO getStats(Integer companyId);
}
