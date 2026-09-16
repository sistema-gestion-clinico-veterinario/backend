package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class AntecedentesRequest {

    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String enfermedades;

    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String procedimientos;

    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String antecedentesPersonales;

    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String antecedentesFamiliares;

    @Size(max = 20, message = "El grupo sanguineo no debe superar 20 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El grupo sanguineo contiene caracteres no permitidos")
    private String grupoSanguineo;
}
