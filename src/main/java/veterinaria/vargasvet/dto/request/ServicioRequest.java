package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.math.BigDecimal;

@Data
public class ServicioRequest {

    private Integer companyId;

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @MeaningfulText(message = "El nombre del servicio debe contener texto real")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 300, message = "La descripcion no debe superar 300 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La descripcion contiene caracteres no permitidos")
    @MeaningfulText(message = "La descripcion debe contener texto real, no solo numeros o simbolos")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "5.00", message = "El precio minimo es S/ 5.00")
    @DecimalMax(value = "5000.00", message = "El precio no debe superar S/ 5000.00")
    private BigDecimal precio;

    private Boolean disponible = true;

    @NotNull(message = "La duración estimada es obligatoria")
    @Min(value = 5, message = "La duracion minima es 5 minutos")
    @Max(value = 240, message = "La duracion maxima es 240 minutos")
    private Integer duracionEstimada;

    private Boolean permiteEmergencia = false;

    private Long tipoEmpleadoId;
}
