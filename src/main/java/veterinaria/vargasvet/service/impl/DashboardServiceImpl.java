package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.dto.response.DashboardStatsDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.DashboardService;

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

        // Calcular estadísticas reales de citas para gráficos
        java.time.LocalDate now = veterinaria.vargasvet.util.AppClock.today();
        
        // 1. Citas por Día (Semana actual: Lunes a Domingo)
        java.time.LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.util.List<Long> citasPorDia = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate day = startOfWeek.plusDays(i);
            java.time.LocalDateTime start = day.atStartOfDay();
            java.time.LocalDateTime end = day.atTime(java.time.LocalTime.MAX);
            if (targetCompanyId != null) {
                citasPorDia.add(citaRepository.countByCompanyAndDateRange(targetCompanyId, start, end));
            } else {
                citasPorDia.add(citaRepository.countGlobalByDateRange(start, end));
            }
        }
        builder.citasPorDia(citasPorDia);

        // 2. Citas por Semana (Semanas del mes actual)
        java.time.LocalDate firstDayOfMonth = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        java.util.List<Long> citasPorSemana = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            java.time.LocalDate startDay = firstDayOfMonth.plusDays(i * 7);
            java.time.LocalDate endDay = (i == 3) ? now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()) : firstDayOfMonth.plusDays((i + 1) * 7 - 1);
            java.time.LocalDateTime start = startDay.atStartOfDay();
            java.time.LocalDateTime end = endDay.atTime(java.time.LocalTime.MAX);
            if (targetCompanyId != null) {
                citasPorSemana.add(citaRepository.countByCompanyAndDateRange(targetCompanyId, start, end));
            } else {
                citasPorSemana.add(citaRepository.countGlobalByDateRange(start, end));
            }
        }
        builder.citasPorSemana(citasPorSemana);

        // 3. Citas por Mes (12 meses del año actual)
        int currentYear = now.getYear();
        java.util.List<Long> citasPorMes = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            java.time.LocalDate firstDay = java.time.LocalDate.of(currentYear, m, 1);
            java.time.LocalDate lastDay = firstDay.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            java.time.LocalDateTime start = firstDay.atStartOfDay();
            java.time.LocalDateTime end = lastDay.atTime(java.time.LocalTime.MAX);
            if (targetCompanyId != null) {
                citasPorMes.add(citaRepository.countByCompanyAndDateRange(targetCompanyId, start, end));
            } else {
                citasPorMes.add(citaRepository.countGlobalByDateRange(start, end));
            }
        }
        builder.citasPorMes(citasPorMes);

        return builder.build();
    }
}
