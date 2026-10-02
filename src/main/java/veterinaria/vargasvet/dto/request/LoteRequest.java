package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoteRequest {

    private Integer companyId;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotBlank(message = "El número de lote es obligatorio")
    @Size(min = 1, max = 60, message = "El número de lote debe tener entre 1 y 60 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El número de lote solo puede contener letras, números, guiones y guiones bajos")
    private String numeroLote;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    private LocalDate fechaIngreso;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad recibida debe ser mayor que cero")
    private Integer cantidad;

    @DecimalMin(value = "0.0", message = "El costo unitario no puede ser negativo")
    private BigDecimal costoUnitario;
}
