package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.DashboardStatsDTO;

public interface DashboardService {
    DashboardStatsDTO getStats(Integer companyId);
}
