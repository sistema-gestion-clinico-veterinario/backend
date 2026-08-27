package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.dto.response.DashboardOverviewDTO;
import veterinaria.vargasvet.dto.response.DashboardStatsDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.DashboardService;
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.service.CompanyService;
import veterinaria.vargasvet.service.EmpleadoService;
import veterinaria.vargasvet.service.MascotaService;
import veterinaria.vargasvet.service.PagoService;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final EmpleadoService empleadoService;
    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final RoleService roleService;
    private final ApoderadoService apoderadoService;
    private final PagoService pagoService;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewDTO getOverview(Integer companyId) {
        Integer targetCompanyId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();
        boolean globalMode = SecurityUtils.isSuperAdmin() && targetCompanyId == null;

        var recentLogs = auditLogService.getLogs(
                targetCompanyId, null, null, null, null, null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))).getContent();
        var companies = (SecurityUtils.isSuperAdmin() || SecurityUtils.hasAuthority("VISTA_COMPANY"))
                ? companyService.listarTodas(0, 200).getContent()
                : List.<veterinaria.vargasvet.dto.response.CompanyListResponse>of();

        if (globalMode) {
            return DashboardOverviewDTO.builder()
                    .stats(getStats(null))
                    .recentLogs(recentLogs)
                    .employees(List.of())
                    .todayAppointments(List.of())
                    .pets(List.of())
                    .roles(List.of())
                    .guardians(List.of())
                    .schedules(List.of())
                    .payments(List.of())
                    .companies(companies)
                    .build();
        }

        var today = veterinaria.vargasvet.util.AppClock.today();
        return DashboardOverviewDTO.builder()
                .stats(getStats(targetCompanyId))
                .recentLogs(recentLogs)
                .employees(empleadoService.listar(targetCompanyId, null, null, null,
                        null, null, 0, 5).getContent())
                .todayAppointments(citaService.listar(targetCompanyId, today, null, null,
                        null, null, 0, 30).getContent())
                .pets(mascotaService.listar(targetCompanyId, null, null, null,
                        null, 0, 6).getContent())
                .roles(roleService.getRolesByCompany(targetCompanyId))
                .guardians(apoderadoService.listar(targetCompanyId, null, null, 0, 5).getContent())
                .schedules(empleadoService.getSchedulesReport(targetCompanyId))
                .payments(pagoService.listarHistorialPorEmpresa(0, 5, targetCompanyId).getContent())
                .companies(companies)
                .build();
    }

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
            builder.totalCitasHoy(citaRepository.countGlobalByDateRange(
                    veterinaria.vargasvet.util.AppClock.today().atStartOfDay(),
                    veterinaria.vargasvet.util.AppClock.today().atTime(java.time.LocalTime.MAX)
            ));
            builder.totalEmpleados(empleadoRepository.count());
        }

        if (SecurityUtils.isSuperAdmin()) {
            builder.totalEmpresas(companyRepository.count());
        }

        java.time.LocalDate now = veterinaria.vargasvet.util.AppClock.today();
        java.time.LocalDate startOfWeek = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate startOfYear = java.time.LocalDate.of(now.getYear(), 1, 1);
        java.time.LocalDate queryStart = startOfWeek.isBefore(startOfYear) ? startOfWeek : startOfYear;
        java.time.LocalDate queryEnd = startOfYear.plusYears(1);
        java.util.List<CitaRepository.FechaCount> dailyCounts = targetCompanyId != null
                ? citaRepository.countDailyByCompanyAndDateRange(targetCompanyId, queryStart.atStartOfDay(), queryEnd.atStartOfDay())
                : citaRepository.countDailyGlobalByDateRange(queryStart.atStartOfDay(), queryEnd.atStartOfDay());
        java.util.Map<java.time.LocalDate, Long> countsByDate = dailyCounts.stream().collect(
                java.util.stream.Collectors.toMap(CitaRepository.FechaCount::getFecha,
                        CitaRepository.FechaCount::getTotal));

        java.util.List<Long> citasPorDia = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            java.time.LocalDate day = startOfWeek.plusDays(i);
            citasPorDia.add(countsByDate.getOrDefault(day, 0L));
        }
        builder.citasPorDia(citasPorDia);

        java.time.LocalDate firstDayOfMonth = now.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        java.util.List<Long> citasPorSemana = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            java.time.LocalDate startDay = firstDayOfMonth.plusDays(i * 7);
            java.time.LocalDate endDay = (i == 3) ? now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()) : firstDayOfMonth.plusDays((i + 1) * 7 - 1);
            long total = countsByDate.entrySet().stream()
                    .filter(entry -> !entry.getKey().isBefore(startDay) && !entry.getKey().isAfter(endDay))
                    .mapToLong(java.util.Map.Entry::getValue).sum();
            citasPorSemana.add(total);
        }
        builder.citasPorSemana(citasPorSemana);

        int currentYear = now.getYear();
        java.util.List<Long> citasPorMes = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            long total = countsByDate.entrySet().stream()
                    .filter(entry -> entry.getKey().getYear() == currentYear && entry.getKey().getMonthValue() == month)
                    .mapToLong(java.util.Map.Entry::getValue).sum();
            citasPorMes.add(total);
        }
        builder.citasPorMes(citasPorMes);

        return builder.build();
    }
}
