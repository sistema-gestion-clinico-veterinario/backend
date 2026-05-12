package veterinaria.vargasvet.modules.company.service;

import veterinaria.vargasvet.dto.response.DashboardStatsDTO;

public interface DashboardService {
    DashboardStatsDTO getStats(Integer companyId);
}
