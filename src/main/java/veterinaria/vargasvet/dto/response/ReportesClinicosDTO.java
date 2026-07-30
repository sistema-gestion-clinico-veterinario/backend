package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReportesClinicosDTO {
    private String fechaDesde;
    private String fechaHasta;
    private Resumen resumen;
    private Resumen resumenAnterior;
    private List<ItemCount> consultasPorTipo;
    private List<ItemCount> diagnosticosPorTipoYEstado;
    private List<ItemCount> tratamientosPorEstado;
    private List<ItemCount> consultasPorEstado;
    private List<ItemCount> pacientesPorEspecie;
    private List<ItemCount> pacientesPorRangoEdad;
    private List<ProximaAplicacion> proximasVacunas;
    private List<ProximaAplicacion> proximasDesparasitaciones;
    private List<ItemCount> consultasPorMes;
    private List<ItemCount> consultasPorVeterinario;
    private List<ItemCount> frecuenciaConsultasPorPaciente;
    private List<ProximaAplicacion> controlesPreventivosProximos;
    private List<ItemCount> serviciosMasSolicitados;
    private List<HeatmapItem> demandaPorHorario;

    @Data
    @Builder
    public static class Resumen {
        private long consultas;
        private long pacientesAtendidos;
        private BigDecimal ingresos;
        private long nuevosPacientes;
        private long tiempoPromedioAtencionMinutos;
        private double porcentajeCitasCompletadas;
    }

    @Data
    @Builder
    public static class ItemCount {
        private String label;
        private long count;
    }

    @Data
    @Builder
    public static class ProximaAplicacion {
        private String mascota;
        private String producto;
        private String fechaProxima;
        private String tipoControl;
    }

    @Data
    @Builder
    public static class HeatmapItem {
        private int diaSemana;
        private int hora;
        private long count;
    }
}
