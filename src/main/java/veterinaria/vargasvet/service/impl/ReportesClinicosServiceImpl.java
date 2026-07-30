package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.*;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ReportesClinicosService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportesClinicosServiceImpl implements ReportesClinicosService {

    private final ConsultaRepository consultaRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final TratamientoRepository tratamientoRepository;
    private final MascotaRepository mascotaRepository;
    private final RegistroVacunaRepository registroVacunaRepository;
    private final RegistroDesparasitacionRepository registroDesparasitacionRepository;

    @Override
    public ReportesClinicosDTO obtenerReportes(Integer companyId) {
        Integer targetCompanyId = companyId;
        if (targetCompanyId == null) {
            targetCompanyId = SecurityUtils.getCurrentCompanyId();
        }
        if (targetCompanyId == null) {
            return ReportesClinicosDTO.builder()
                    .consultasPorTipo(List.of())
                    .diagnosticosPorTipoYEstado(List.of())
                    .tratamientosPorEstado(List.of())
                    .consultasPorEstado(List.of())
                    .pacientesPorEspecie(List.of())
                    .pacientesPorRangoEdad(List.of())
                    .proximasVacunas(List.of())
                    .proximasDesparasitaciones(List.of())
                    .build();
        }

        List<ReportesClinicosDTO.ItemCount> consultasPorTipo = toItemCounts(
                consultaRepository.countByTipoConsulta(targetCompanyId));

        List<ReportesClinicosDTO.ItemCount> diagnosticos = toItemCounts(
                diagnosticoRepository.countByTipoYEstado(targetCompanyId));

        List<ReportesClinicosDTO.ItemCount> tratamientos = toItemCounts(
                tratamientoRepository.countPorEstado(targetCompanyId));

        List<ReportesClinicosDTO.ItemCount> consultasPorEstado = toItemCounts(
                consultaRepository.countPorEstado(targetCompanyId));

        List<ReportesClinicosDTO.ItemCount> especies = toItemCounts(
                mascotaRepository.countByEspecie(targetCompanyId));

        List<ReportesClinicosDTO.ItemCount> rangosEdad = calcularRangosEdad(targetCompanyId);

        List<ReportesClinicosDTO.ProximaAplicacion> proximasVacunas = findProximasVacunas(targetCompanyId);
        List<ReportesClinicosDTO.ProximaAplicacion> proximasDesparasitaciones = findProximasDesparasitaciones(targetCompanyId);

        return ReportesClinicosDTO.builder()
                .consultasPorTipo(consultasPorTipo)
                .diagnosticosPorTipoYEstado(diagnosticos)
                .tratamientosPorEstado(tratamientos)
                .consultasPorEstado(consultasPorEstado)
                .pacientesPorEspecie(especies)
                .pacientesPorRangoEdad(rangosEdad)
                .proximasVacunas(proximasVacunas)
                .proximasDesparasitaciones(proximasDesparasitaciones)
                .build();
    }

    private List<ReportesClinicosDTO.ItemCount> toItemCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> {
                    String label = row[0] != null ? row[0].toString().replace("_", " ") : "Sin registro";
                    long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0;
                    return ReportesClinicosDTO.ItemCount.builder().label(label).count(count).build();
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    private List<ReportesClinicosDTO.ItemCount> calcularRangosEdad(Integer companyId) {
        List<LocalDate> fechas = mascotaRepository.findFechasNacimientoByCompany(companyId);
        LocalDate hoy = LocalDate.now();

        Map<String, Long> rangos = new LinkedHashMap<>();
        rangos.put("Cachorro (< 1 año)", 0L);
        rangos.put("Joven (1-3 años)", 0L);
        rangos.put("Adulto (4-7 años)", 0L);
        rangos.put("Senior (8-10 años)", 0L);
        rangos.put("Geriátrico (> 10 años)", 0L);
        rangos.put("Sin registro", 0L);

        for (LocalDate fn : fechas) {
            if (fn == null) {
                rangos.merge("Sin registro", 1L, Long::sum);
                continue;
            }
            long edad = ChronoUnit.YEARS.between(fn, hoy);
            if (edad < 1) rangos.merge("Cachorro (< 1 año)", 1L, Long::sum);
            else if (edad <= 3) rangos.merge("Joven (1-3 años)", 1L, Long::sum);
            else if (edad <= 7) rangos.merge("Adulto (4-7 años)", 1L, Long::sum);
            else if (edad <= 10) rangos.merge("Senior (8-10 años)", 1L, Long::sum);
            else rangos.merge("Geriátrico (> 10 años)", 1L, Long::sum);
        }

        return rangos.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> ReportesClinicosDTO.ItemCount.builder().label(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasVacunas(Integer companyId) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusMonths(1);
        return registroVacunaRepository.findProximasVacunas(companyId, hoy, limite).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getNombreVacuna())
                        .fechaProxima(r.getFechaProximaDosis().toString())
                        .build())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasDesparasitaciones(Integer companyId) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusMonths(1);
        return registroDesparasitacionRepository.findProximasDesparasitaciones(companyId, hoy, limite).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getProducto())
                        .fechaProxima(r.getFechaProximaAplicacion().toString())
                        .build())
                .limit(10)
                .collect(Collectors.toList());
    }
}
