package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.domain.enums.TipoDiagnostico;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDate;

@Data
public class DiagnosticoRequest {

    @NotBlank(message = "El nombre del diagnóstico es obligatorio")
    @Size(max = 200, message = "El nombre no debe superar 200 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El nombre contiene caracteres no permitidos")
    @MeaningfulText(message = "El nombre debe contener texto real, no solo numeros o simbolos")
    private String nombre;

    @Size(max = 20, message = "El código CIE no debe superar 20 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El código CIE contiene caracteres no permitidos")
    private String codigoCIE;

    @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La descripción contiene caracteres no permitidos")
    private String descripcion;

    @NotNull(message = "El tipo de diagnóstico es obligatorio")
    private TipoDiagnostico tipo;

    @NotNull(message = "El estado del diagnóstico es obligatorio")
    private EstadoDiagnostico estado;

    @FutureOrPresent(message = "La fecha de próximo control no puede ser pasada")
    private LocalDate fechaProximoControl;
}
