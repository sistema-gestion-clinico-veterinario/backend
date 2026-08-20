package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;
import java.math.BigDecimal;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;

@Data
@Builder
public class AplicacionPreventivaResponse {
    private Long id;
    private TipoControlPreventivo tipo;
    private String nombreControl;
    private LocalDate fechaAplicacion;
    private Integer periodicidadMeses;
    private Integer intervaloCantidad;
    private IntervaloUnidad intervaloUnidad;
    private LocalDate fechaProximaAplicacion;
    private String veterinarioNombre;
    private String lote;
    private LocalDate fechaVencimientoProducto;
    private BigDecimal dosis;
    private String unidadDosis;
    private String viaAdministracion;
    private String sitioAplicacion;
    private BigDecimal pesoKg;
    private String observaciones;
}
