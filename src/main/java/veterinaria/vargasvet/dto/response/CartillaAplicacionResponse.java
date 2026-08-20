package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;

@Data
@Builder
public class CartillaAplicacionResponse {
    private Long registroId;
    private TipoControlPreventivo tipo;
    private Long mascotaId;
    private String mascotaNombre;
    private String numeroHc;
    private String nombre;          // nombre vacuna o producto
    private LocalDate fechaAplicacion;
    private LocalDate fechaProxima;
    private Integer periodicidadMeses;
    private Integer intervaloCantidad;
    private IntervaloUnidad intervaloUnidad;
    private String veterinarioNombre;
    private String lote;
    private LocalDate fechaVencimientoProducto;
    private BigDecimal dosis;
    private String unidadDosis;
    private String viaAdministracion;
    private String sitioAplicacion;
    private BigDecimal pesoKg;
    private String observaciones;
    private Long citaId;            // cita-cobro interna
    private String codigoCobro;     // numero_cita que aparece en Caja
    private BigDecimal total;       // monto del cobro
}
