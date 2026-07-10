package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDate;

@Data
public class PrescripcionRequest {

    @NotBlank(message = "El medicamento es obligatorio")
    @Size(max = 80, message = "El medicamento no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El medicamento contiene caracteres no permitidos")
    @MeaningfulText(message = "El medicamento debe contener texto real, no solo numeros o simbolos")
    private String medicamento;

    @Size(max = 80, message = "El principio activo no debe superar 80 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El principio activo contiene caracteres no permitidos")
    @MeaningfulText(message = "El principio activo debe contener texto real, no solo numeros o simbolos")
    private String principioActivo;

    @NotBlank(message = "La dosis es obligatoria")
    @Size(max = 80, message = "La dosis no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La dosis contiene caracteres no permitidos")
    @MeaningfulText(message = "La dosis debe contener unidades o texto, no solo numeros o simbolos")
    private String dosis;

    @NotBlank(message = "La frecuencia es obligatoria")
    @Size(max = 80, message = "La frecuencia no debe superar 80 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La frecuencia contiene caracteres no permitidos")
    @MeaningfulText(message = "La frecuencia debe contener texto real, no solo numeros o simbolos")
    private String frecuencia;

    @Min(value = 1, message = "La duracion debe ser al menos de 1 dia")
    @Max(value = 365, message = "La duracion no debe superar 365 dias")
    private Integer duracionDias;

    @NotBlank(message = "La vía de administración es obligatoria")
    @Size(max = 50, message = "La via no debe superar 50 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La via contiene caracteres no permitidos")
    @MeaningfulText(message = "La via de administracion debe contener texto real")
    private String viaAdministracion;

    @Size(max = 500, message = "Las instrucciones no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las instrucciones contienen caracteres no permitidos")
    @MeaningfulText(message = "Las instrucciones deben contener texto real, no solo numeros o simbolos")
    private String instrucciones;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser pasada")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
