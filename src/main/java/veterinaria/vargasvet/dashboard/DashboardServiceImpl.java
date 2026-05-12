package veterinaria.vargasvet.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.dashboard.DashboardStatsDTO;
import veterinaria.vargasvet.citas.CitaRepository;
import veterinaria.vargasvet.admin.CompanyRepository;
import veterinaria.vargasvet.admin.EmpleadoRepository;
import veterinaria.vargasvet.pacientes.MascotaRepository;
import veterinaria.vargasvet.shared.SecurityUtils;
import veterinaria.vargasvet.dashboard.DashboardService;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CompanyRepository companyRepository;

    @Override
    public DashboardStatsDTO getStats(Integer companyId) {
        Integer targetCompanyId;
        
        if (SecurityUtils.isSuperAdmin()) {
            targetCompanyId = companyId;
        } else {
            targetCompanyId = SecurityUtils.getCurrentCompanyId();
        }

        DashboardStatsDTO.DashboardStatsDTOBuilder builder = DashboardStatsDTO.builder();

        if (targetCompanyId != null) {
            builder.totalPacientes(mascotaRepository.countByCompanyId(targetCompanyId));
            builder.totalCitasHoy(citaRepository.countTodayByCompanyId(targetCompanyId));
            builder.totalCitas(citaRepository.countByCompanyId(targetCompanyId));
            builder.totalEmpleados(empleadoRepository.countByCompanyId(targetCompanyId));
        } else if (SecurityUtils.isSuperAdmin()) {
            // Stats globales para SuperAdmin si no hay empresa seleccionada
            builder.totalPacientes(mascotaRepository.count());
            builder.totalCitas(citaRepository.count());
            builder.totalEmpleados(empleadoRepository.count());
        }

        if (SecurityUtils.isSuperAdmin()) {
            builder.totalEmpresas(companyRepository.count());
        }

        return builder.build();
    }
}
