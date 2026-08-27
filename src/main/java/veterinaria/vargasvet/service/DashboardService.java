package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.DashboardStatsDTO;
import veterinaria.vargasvet.dto.response.DashboardOverviewDTO;

public interface DashboardService {
    DashboardStatsDTO getStats(Integer companyId);
    DashboardOverviewDTO getOverview(Integer companyId);
}
