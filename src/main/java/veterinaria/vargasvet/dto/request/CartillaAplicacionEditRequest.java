package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CartillaAplicacionEditRequest {
    @NotNull(message = "El ID del registro es obligatorio")
    private Long registroId;

    private LocalDate fechaAplicacion;
    private String lote;
    private LocalDate fechaVencimientoProducto;
    private BigDecimal dosis;
    private String unidadDosis;
    private String viaAdministracion;
    private String sitioAplicacion;
    private BigDecimal pesoKg;
    private String observaciones;
    private Integer intervaloCantidad;
    private String intervaloUnidad;
}
