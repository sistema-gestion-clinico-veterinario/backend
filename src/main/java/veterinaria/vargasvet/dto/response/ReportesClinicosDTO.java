package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ReportesClinicosDTO {
    private List<ItemCount> consultasPorTipo;
    private List<ItemCount> diagnosticosPorTipoYEstado;
    private List<ItemCount> tratamientosPorEstado;
    private List<ItemCount> consultasPorEstado;
    private List<ItemCount> pacientesPorEspecie;
    private List<ItemCount> pacientesPorRangoEdad;
    private List<ProximaAplicacion> proximasVacunas;
    private List<ProximaAplicacion> proximasDesparasitaciones;

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
    }
}
