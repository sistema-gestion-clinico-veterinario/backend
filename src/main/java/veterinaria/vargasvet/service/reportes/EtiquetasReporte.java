package veterinaria.vargasvet.service.reportes;

import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

final class EtiquetasReporte {

    private static final Locale PERU = new Locale("es", "PE");

    private EtiquetasReporte() {
    }

    static ReportesClinicosDTO.ItemCount item(String etiqueta, long cantidad) {
        return ReportesClinicosDTO.ItemCount.builder().label(etiqueta).count(cantidad).build();
    }

    static <T> List<ReportesClinicosDTO.ItemCount> contarPor(List<T> elementos, Function<T, String> etiqueta) {
        return elementos.stream()
                .collect(Collectors.groupingBy(etiqueta, Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    static String humanizar(String valor) {
        if ("NO_ASISTIO".equals(valor)) return "No asistió";
        String normalizado = valor.replace("_", " ").toLowerCase(PERU);
        return Character.toUpperCase(normalizado.charAt(0)) + normalizado.substring(1);
    }

    static String servicio(Cita cita) {
        return cita.getServicio() != null && cita.getServicio().getNombre() != null
                ? cita.getServicio().getNombre()
                : "Sin servicio";
    }

    static String tipoConsulta(Cita cita) {
        return cita.getConsulta() != null && cita.getConsulta().getTipoConsulta() != null
                ? humanizar(cita.getConsulta().getTipoConsulta().name())
                : servicio(cita);
    }

    static String veterinario(Cita cita) {
        if (cita.getEmpleado() == null || cita.getEmpleado().getUser() == null) return "Sin asignar";
        String nombre = cita.getEmpleado().getUser().getNombre();
        String apellido = cita.getEmpleado().getUser().getApellido();
        return ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
    }
}
