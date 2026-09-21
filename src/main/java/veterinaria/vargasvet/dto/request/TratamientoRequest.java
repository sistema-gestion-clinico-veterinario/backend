package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoTratamiento;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDate;

@Data
public class TratamientoRequest {

    @NotBlank(message = "El nombre del tratamiento es obligatorio")
    @Size(max = 200, message = "El nombre no debe superar 200 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El nombre contiene caracteres no permitidos")
    @MeaningfulText(message = "El nombre debe contener texto real, no solo numeros o simbolos")
    private String nombre;

    @Size(max = 1000, message = "La descripción no debe superar 1000 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La descripción contiene caracteres no permitidos")
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @NotNull(message = "El estado del tratamiento es obligatorio")
    private EstadoTratamiento estado;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
