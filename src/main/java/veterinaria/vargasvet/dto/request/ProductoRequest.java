package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    private Integer companyId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 2, max = 160, message = "El nombre debe tener entre 2 y 160 caracteres")
    @MeaningfulText(message = "El nombre del producto debe contener texto real")
    private String nombre;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.10", message = "El precio mínimo es S/ 0.10")
    @DecimalMax(value = "5000.00", message = "El precio no debe superar S/ 5000.00")
    private BigDecimal precio;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock = 0;

    @Size(max = 300, message = "La descripción no debe superar 300 caracteres")
    @Pattern(regexp = "^$|^(?=.*[\\p{L}\\p{N}])(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*$", message = "La descripción contiene caracteres no permitidos")
    private String descripcion;

    @Size(max = 500, message = "La URL de la imagen no debe superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://[^\\s<>]+$", message = "La URL de la imagen no es válida")
    private String imagenUrl;
}
