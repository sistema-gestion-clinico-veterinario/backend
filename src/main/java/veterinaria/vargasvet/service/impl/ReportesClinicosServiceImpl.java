package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ReportesClinicosService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
//este es el proyecto
@Service
@RequiredArgsConstructor
public class ReportesClinicosServiceImpl implements ReportesClinicosService {

    private final CitaRepository citaRepository;
    private final RegistroVacunaRepository registroVacunaRepository;
    private final RegistroDesparasitacionRepository registroDesparasitacionRepository;
    private final ControlPreventivoRepository controlPreventivoRepository;

    @Override
    public ReportesClinicosDTO obtenerReportes(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta,
                                               Long veterinarioId, EspecieMascota especie) {
        Integer targetCompanyId = resolveCompanyId(companyId);
        LocalDate hoy = AppClock.today();
        LocalDate desde = fechaDesde != null ? fechaDesde : hoy.withDayOfMonth(1);
        LocalDate hasta = fechaHasta != null ? fechaHasta : hoy;
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > 1110) {
            throw new IllegalArgumentException("El rango máximo permitido para reportes es de 36 meses");
        }

        if (targetCompanyId == null) {
            return emptyReport(desde, hasta);
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate anteriorHasta = desde.minusDays(1);
        LocalDate anteriorDesde = anteriorHasta.minusDays(dias - 1);

        List<Cita> actuales = citaRepository.findForClinicalReport(
                targetCompanyId, inicio, finExclusivo, veterinarioId, especie);
        List<Cita> anteriores = citaRepository.findForClinicalReport(
                targetCompanyId, anteriorDesde.atStartOfDay(), desde.atStartOfDay(), veterinarioId, especie);

        List<Mascota> pacientes = new ArrayList<>(actuales.stream()
                .map(Cita::getMascota)
                .collect(Collectors.toMap(
                        Mascota::getId,
                        Function.identity(),
                        (primera, repetida) -> primera,
                        LinkedHashMap::new))
                .values());

        return ReportesClinicosDTO.builder()
                .fechaDesde(desde.toString())
                .fechaHasta(hasta.toString())
                .resumen(calcularResumen(actuales, desde, hasta))
                .resumenAnterior(calcularResumen(anteriores, anteriorDesde, anteriorHasta))
                .consultasPorTipo(group(actuales, this::tipoConsultaLabel))
                .diagnosticosPorTipoYEstado(List.of())
                .tratamientosPorEstado(List.of())
                .consultasPorEstado(group(actuales, c -> humanize(c.getEstado().name())))
                .pacientesPorEspecie(groupMascotas(pacientes, m -> humanize(m.getEspecie().name())))
                .pacientesPorRangoEdad(calcularRangosEdad(pacientes, hasta))
                .consultasPorMes(calcularSerie(actuales, desde, hasta))
                .consultasPorVeterinario(group(actuales, this::nombreVeterinario))
                .frecuenciaConsultasPorPaciente(calcularFrecuencia(actuales))
                .serviciosMasSolicitados(group(actuales, this::servicioLabel))
                .demandaPorHorario(calcularDemanda(actuales))
                .proximasVacunas(findProximasVacunas(targetCompanyId))
                .proximasDesparasitaciones(findProximasDesparasitaciones(targetCompanyId))
                .controlesPreventivosProximos(findControlesPreventivosProximos(targetCompanyId))
                .build();
    }

    private Integer resolveCompanyId(Integer requestedCompanyId) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestedCompanyId;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    private ReportesClinicosDTO.Resumen calcularResumen(List<Cita> citas, LocalDate desde, LocalDate hasta) {
        Set<Long> pacientesAtendidos = citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.COMPLETADA || c.getEstado() == EstadoCita.EN_PROCESO)
                .map(c -> c.getMascota().getId())
                .collect(Collectors.toSet());

        BigDecimal ingresos = citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .map(this::importeReal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long nuevosPacientes = citas.stream()
                .map(Cita::getMascota)
                .filter(m -> m.getCreatedAt() != null)
                .filter(m -> !m.getCreatedAt().toLocalDate().isBefore(desde)
                        && !m.getCreatedAt().toLocalDate().isAfter(hasta))
                .map(Mascota::getId)
                .distinct()
                .count();

        List<Long> duraciones = citas.stream()
                .filter(c -> c.getConsulta() != null)
                .filter(c -> c.getConsulta().getFechaConsulta() != null && c.getConsulta().getFechaCierre() != null)
                .map(c -> ChronoUnit.MINUTES.between(
                        c.getConsulta().getFechaConsulta(), c.getConsulta().getFechaCierre()))
                .filter(minutos -> minutos >= 0 && minutos <= 24 * 60)
                .toList();
        long promedio = duraciones.isEmpty()
                ? 0
                : Math.round(duraciones.stream().mapToLong(Long::longValue).average().orElse(0));

        long cerradas = citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.COMPLETADA
                        || c.getEstado() == EstadoCita.CANCELADA
                        || c.getEstado() == EstadoCita.NO_ASISTIO)
                .count();
        long completadas = citas.stream().filter(c -> c.getEstado() == EstadoCita.COMPLETADA).count();
        double porcentaje = cerradas == 0 ? 0 : (completadas * 100.0 / cerradas);

        return ReportesClinicosDTO.Resumen.builder()
                .consultas(citas.size())
                .pacientesAtendidos(pacientesAtendidos.size())
                .ingresos(ingresos.setScale(2, RoundingMode.HALF_UP))
                .nuevosPacientes(nuevosPacientes)
                .tiempoPromedioAtencionMinutos(promedio)
                .porcentajeCitasCompletadas(Math.round(porcentaje * 10.0) / 10.0)
                .build();
    }

    private BigDecimal importeReal(Cita cita) {
        BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        return pagado.min(total).max(BigDecimal.ZERO);
    }

    private List<ReportesClinicosDTO.ItemCount> calcularSerie(
            List<Cita> citas, LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        Map<String, Long> values = new LinkedHashMap<>();
        Function<Cita, String> keyFn;

        if (dias == 1) {
            for (int hora = 7; hora <= 20; hora++) values.put(String.format("%02d:00", hora), 0L);
            keyFn = c -> String.format("%02d:00", c.getFechaHoraInicio().getHour());
        } else if (dias <= 45) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "PE"));
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) values.put(d.format(formatter), 0L);
            keyFn = c -> c.getFechaHoraInicio().toLocalDate().format(formatter);
        } else {
            DateTimeFormatter key = DateTimeFormatter.ofPattern("yyyy-MM");
            DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yy", new Locale("es", "PE"));
            for (LocalDate d = desde.withDayOfMonth(1); !d.isAfter(hasta); d = d.plusMonths(1)) {
                values.put(d.format(key), 0L);
            }
            for (Cita cita : citas) values.merge(cita.getFechaHoraInicio().format(key), 1L, Long::sum);
            return values.entrySet().stream()
                    .map(e -> item(LocalDate.parse(e.getKey() + "-01").format(label), e.getValue()))
                    .toList();
        }

        for (Cita cita : citas) {
            String key = keyFn.apply(cita);
            if (values.containsKey(key)) values.merge(key, 1L, Long::sum);
        }
        return values.entrySet().stream().map(e -> item(e.getKey(), e.getValue())).toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularRangosEdad(List<Mascota> mascotas, LocalDate referencia) {
        Map<String, Long> rangos = new LinkedHashMap<>();
        rangos.put("Cachorro (0-1 año)", 0L);
        rangos.put("Joven (1-3 años)", 0L);
        rangos.put("Adulto (4-7 años)", 0L);
        rangos.put("Senior (8+ años)", 0L);
        rangos.put("Sin registro", 0L);

        for (Mascota mascota : mascotas) {
            if (mascota.getFechaNacimiento() == null) {
                rangos.merge("Sin registro", 1L, Long::sum);
                continue;
            }
            long edad = ChronoUnit.YEARS.between(mascota.getFechaNacimiento(), referencia);
            if (edad < 1) rangos.merge("Cachorro (0-1 año)", 1L, Long::sum);
            else if (edad <= 3) rangos.merge("Joven (1-3 años)", 1L, Long::sum);
            else if (edad <= 7) rangos.merge("Adulto (4-7 años)", 1L, Long::sum);
            else rangos.merge("Senior (8+ años)", 1L, Long::sum);
        }
        return rangos.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> item(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularFrecuencia(List<Cita> citas) {
        Map<Long, Long> porMascota = citas.stream()
                .collect(Collectors.groupingBy(c -> c.getMascota().getId(), Collectors.counting()));
        long una = porMascota.values().stream().filter(c -> c == 1).count();
        long dosTres = porMascota.values().stream().filter(c -> c >= 2 && c <= 3).count();
        long cuatroMas = porMascota.values().stream().filter(c -> c >= 4).count();
        List<ReportesClinicosDTO.ItemCount> result = new ArrayList<>();
        if (una > 0) result.add(item("1 cita", una));
        if (dosTres > 0) result.add(item("2-3 citas", dosTres));
        if (cuatroMas > 0) result.add(item("4+ citas", cuatroMas));
        return result;
    }

    private List<ReportesClinicosDTO.HeatmapItem> calcularDemanda(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .collect(Collectors.groupingBy(c -> c.getFechaHoraInicio().getDayOfWeek().getValue()
                        + "-" + c.getFechaHoraInicio().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    String[] key = e.getKey().split("-");
                    return ReportesClinicosDTO.HeatmapItem.builder()
                            .diaSemana(Integer.parseInt(key[0]))
                            .hora(Integer.parseInt(key[1]))
                            .count(e.getValue())
                            .build();
                })
                .sorted(Comparator.comparingInt(ReportesClinicosDTO.HeatmapItem::getHora)
                        .thenComparingInt(ReportesClinicosDTO.HeatmapItem::getDiaSemana))
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> group(List<Cita> citas, Function<Cita, String> labelFn) {
        return citas.stream()
                .collect(Collectors.groupingBy(labelFn, Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> groupMascotas(
            List<Mascota> mascotas, Function<Mascota, String> labelFn) {
        return mascotas.stream()
                .collect(Collectors.groupingBy(labelFn, Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    private String tipoConsultaLabel(Cita cita) {
        return cita.getConsulta() != null && cita.getConsulta().getTipoConsulta() != null
                ? humanize(cita.getConsulta().getTipoConsulta().name())
                : servicioLabel(cita);
    }

    private String servicioLabel(Cita cita) {
        return cita.getServicio() != null && cita.getServicio().getNombre() != null
                ? cita.getServicio().getNombre()
                : "Sin servicio";
    }

    private String nombreVeterinario(Cita cita) {
        if (cita.getEmpleado() == null || cita.getEmpleado().getUser() == null) return "Sin asignar";
        String nombre = cita.getEmpleado().getUser().getNombre();
        String apellido = cita.getEmpleado().getUser().getApellido();
        return ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
    }

    private String humanize(String value) {
        String normalized = value.replace("_", " ").toLowerCase(new Locale("es", "PE"));
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private ReportesClinicosDTO.ItemCount item(String label, long count) {
        return ReportesClinicosDTO.ItemCount.builder().label(label).count(count).build();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasVacunas(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroVacunaRepository.findProximasVacunas(companyId, hoy, hoy.plusDays(30), PageRequest.of(0, 5)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getNombreVacuna())
                        .fechaProxima(r.getFechaProximaDosis().toString())
                        .tipoControl("VACUNACION")
                        .build())
                .toList();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasDesparasitaciones(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroDesparasitacionRepository.findProximasDesparasitaciones(
                        companyId, hoy, hoy.plusDays(30), PageRequest.of(0, 5)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getProducto())
                        .fechaProxima(r.getFechaProximaAplicacion().toString())
                        .tipoControl("DESPARASITACION")
                        .build())
                .toList();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findControlesPreventivosProximos(Integer companyId) {
        LocalDate hoy = AppClock.today();
        List<veterinaria.vargasvet.domain.enums.EstadoControlPreventivo> estados = List.of(
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROGRAMADO,
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROXIMO,
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PENDIENTE);
        return controlPreventivoRepository.findProximosByCompany(
                        companyId, hoy, hoy.plusDays(30), estados, PageRequest.of(0, 5)).stream()
                .map(cp -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(cp.getMascota().getNombreCompleto())
                        .producto(cp.getNombreControl())
                        .fechaProxima(cp.getFechaRecomendada().toString())
                        .tipoControl(cp.getTipo().name())
                        .build())
                .toList();
    }

    private ReportesClinicosDTO emptyReport(LocalDate desde, LocalDate hasta) {
        ReportesClinicosDTO.Resumen cero = ReportesClinicosDTO.Resumen.builder()
                .consultas(0).pacientesAtendidos(0).ingresos(BigDecimal.ZERO)
                .nuevosPacientes(0).tiempoPromedioAtencionMinutos(0)
                .porcentajeCitasCompletadas(0).build();
        return ReportesClinicosDTO.builder()
                .fechaDesde(desde.toString()).fechaHasta(hasta.toString())
                .resumen(cero).resumenAnterior(cero)
                .consultasPorTipo(List.of()).diagnosticosPorTipoYEstado(List.of())
                .tratamientosPorEstado(List.of()).consultasPorEstado(List.of())
                .pacientesPorEspecie(List.of()).pacientesPorRangoEdad(List.of())
                .proximasVacunas(List.of()).proximasDesparasitaciones(List.of())
                .consultasPorMes(List.of()).consultasPorVeterinario(List.of())
                .frecuenciaConsultasPorPaciente(List.of()).controlesPreventivosProximos(List.of())
                .serviciosMasSolicitados(List.of()).demandaPorHorario(List.of())
                .build();
    }
}
