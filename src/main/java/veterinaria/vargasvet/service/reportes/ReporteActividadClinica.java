package veterinaria.vargasvet.service.reportes;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.contarPor;
import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.humanizar;
import static veterinaria.vargasvet.service.reportes.EtiquetasReporte.item;

@Component
public class ReporteActividadClinica {

    private static final int TOP_PACIENTES_FRECUENTES = 10;
    private static final Locale PERU = new Locale("es", "PE");

    public List<Mascota> pacientesDe(List<Cita> citas) {
        return new ArrayList<>(citas.stream()
                .map(Cita::getMascota)
                .collect(Collectors.toMap(
                        Mascota::getId,
                        Function.identity(),
                        (primera, repetida) -> primera,
                        LinkedHashMap::new))
                .values());
    }

    public ReportesClinicosDTO.Resumen resumen(List<Cita> citas, RangoDeReporte rango, boolean puedePagos) {
        LocalDate desde = rango.desde();
        LocalDate hasta = rango.hasta();
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
        long noAsistieron = citas.stream().filter(c -> c.getEstado() == EstadoCita.NO_ASISTIO).count();

        return ReportesClinicosDTO.Resumen.builder()
                .consultas(citas.size())
                .pacientesAtendidos(pacientesAtendidos.size())
                .ingresos(puedePagos ? ingresos.setScale(2, RoundingMode.HALF_UP) : null)
                .nuevosPacientes(nuevosPacientes)
                .tiempoPromedioAtencionMinutos(promedio)
                .porcentajeCitasCompletadas(Math.round(porcentaje * 10.0) / 10.0)
                .noAsistieron(noAsistieron)
                .build();
    }

    public List<ReportesClinicosDTO.ItemCount> consultasPorTipo(List<Cita> citas) {
        return contarPor(citas, EtiquetasReporte::tipoConsulta);
    }

    public List<ReportesClinicosDTO.ItemCount> consultasPorEstado(List<Cita> citas) {
        return contarPor(citas, c -> humanizar(c.getEstado().name()));
    }

    public List<ReportesClinicosDTO.ItemCount> consultasPorVeterinario(List<Cita> citas) {
        return contarPor(citas, EtiquetasReporte::veterinario);
    }

    public List<ReportesClinicosDTO.ItemCount> serviciosMasSolicitados(List<Cita> citas) {
        return contarPor(citas, EtiquetasReporte::servicio);
    }

    public List<ReportesClinicosDTO.ItemCount> pacientesPorEspecie(List<Mascota> pacientes) {
        return contarPor(pacientes, m -> humanizar(m.getEspecie().name()));
    }

    public List<ReportesClinicosDTO.ItemCount> pacientesPorRangoEdad(List<Mascota> mascotas, LocalDate referencia) {
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

    public List<ReportesClinicosDTO.ItemCount> consultasPorPeriodo(List<Cita> citas, RangoDeReporte rango) {
        LocalDate desde = rango.desde();
        LocalDate hasta = rango.hasta();
        long dias = rango.dias();
        Map<String, Long> valores = new LinkedHashMap<>();
        Function<Cita, String> clave;

        if (dias == 1) {
            for (int hora = 7; hora <= 20; hora++) valores.put(String.format("%02d:00", hora), 0L);
            clave = c -> String.format("%02d:00", c.getFechaHoraInicio().getHour());
        } else if (dias <= 45) {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd MMM", PERU);
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) valores.put(d.format(formato), 0L);
            clave = c -> c.getFechaHoraInicio().toLocalDate().format(formato);
        } else {
            DateTimeFormatter formatoClave = DateTimeFormatter.ofPattern("yyyy-MM");
            DateTimeFormatter formatoEtiqueta = DateTimeFormatter.ofPattern("MMM yy", PERU);
            for (LocalDate d = desde.withDayOfMonth(1); !d.isAfter(hasta); d = d.plusMonths(1)) {
                valores.put(d.format(formatoClave), 0L);
            }
            for (Cita cita : citas) valores.merge(cita.getFechaHoraInicio().format(formatoClave), 1L, Long::sum);
            return valores.entrySet().stream()
                    .map(e -> item(LocalDate.parse(e.getKey() + "-01").format(formatoEtiqueta), e.getValue()))
                    .toList();
        }

        for (Cita cita : citas) {
            String key = clave.apply(cita);
            if (valores.containsKey(key)) valores.merge(key, 1L, Long::sum);
        }
        return valores.entrySet().stream().map(e -> item(e.getKey(), e.getValue())).toList();
    }

    public List<ReportesClinicosDTO.ItemCount> frecuenciaPorPaciente(List<Cita> citas) {
        Map<Long, Long> porMascota = citas.stream()
                .collect(Collectors.groupingBy(c -> c.getMascota().getId(), Collectors.counting()));
        long una = porMascota.values().stream().filter(c -> c == 1).count();
        long dosTres = porMascota.values().stream().filter(c -> c >= 2 && c <= 3).count();
        long cuatroMas = porMascota.values().stream().filter(c -> c >= 4).count();
        List<ReportesClinicosDTO.ItemCount> resultado = new ArrayList<>();
        if (una > 0) resultado.add(item("1 cita", una));
        if (dosTres > 0) resultado.add(item("2-3 citas", dosTres));
        if (cuatroMas > 0) resultado.add(item("4+ citas", cuatroMas));
        return resultado;
    }

    public List<ReportesClinicosDTO.HeatmapItem> demandaPorHorario(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .collect(Collectors.groupingBy(c -> c.getFechaHoraInicio().getDayOfWeek().getValue()
                        + "-" + c.getFechaHoraInicio().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    String[] clave = e.getKey().split("-");
                    return ReportesClinicosDTO.HeatmapItem.builder()
                            .diaSemana(Integer.parseInt(clave[0]))
                            .hora(Integer.parseInt(clave[1]))
                            .count(e.getValue())
                            .build();
                })
                .sorted(Comparator.comparingInt(ReportesClinicosDTO.HeatmapItem::getHora)
                        .thenComparingInt(ReportesClinicosDTO.HeatmapItem::getDiaSemana))
                .toList();
    }

    public List<ReportesClinicosDTO.ItemCount> pacientesFrecuentes(List<Cita> citas) {
        return contarPor(
                citas.stream().filter(c -> c.getEstado() == EstadoCita.COMPLETADA).toList(),
                c -> c.getMascota().getNombreCompleto())
                .stream()
                .limit(TOP_PACIENTES_FRECUENTES)
                .toList();
    }

    private BigDecimal importeReal(Cita cita) {
        BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        return pagado.min(total).max(BigDecimal.ZERO);
    }
}
