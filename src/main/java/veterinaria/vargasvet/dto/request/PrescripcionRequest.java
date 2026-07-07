package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PrescripcionRequest {

    @NotBlank(message = "El medicamento es obligatorio")
    @Size(max = 80, message = "El medicamento no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El medicamento contiene caracteres no permitidos")
    private String medicamento;

    @Size(max = 80, message = "El principio activo no debe superar 80 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El principio activo contiene caracteres no permitidos")
    private String principioActivo;

    @NotBlank(message = "La dosis es obligatoria")
    @Size(max = 80, message = "La dosis no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La dosis contiene caracteres no permitidos")
    private String dosis;

    @NotBlank(message = "La frecuencia es obligatoria")
    @Size(max = 80, message = "La frecuencia no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La frecuencia contiene caracteres no permitidos")
    private String frecuencia;

    @Min(value = 1, message = "La duracion debe ser al menos de 1 dia")
    private Integer duracionDias;

    @NotBlank(message = "La vía de administración es obligatoria")
    @Size(max = 50, message = "La via no debe superar 50 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La via contiene caracteres no permitidos")
    private String viaAdministracion;

    @Size(max = 500, message = "Las instrucciones no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las instrucciones contienen caracteres no permitidos")
    private String instrucciones;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
