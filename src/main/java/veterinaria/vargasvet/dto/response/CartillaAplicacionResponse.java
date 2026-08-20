package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private String veterinarioNombre;
    private Long citaId;            // cita-cobro interna
    private String codigoCobro;     // numero_cita que aparece en Caja
    private BigDecimal total;       // monto del cobro
}