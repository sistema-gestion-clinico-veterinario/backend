package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class ConsultaRequest {

    @NotNull(message = "El campo version es requerido para evitar ediciones simultáneas")
    private Long version;

    @NotNull(message = "El tipo de consulta es requerido")
    private TipoConsulta tipoConsulta;

    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
    @DecimalMax(value = "120.0", message = "El peso no debe superar 120 kg")
    private Double pesoEnConsulta;

    @DecimalMin(value = "0.1", message = "La temperatura debe ser mayor a 0")
    @DecimalMax(value = "45.0", message = "La temperatura no debe superar 45 C")
    private Double temperatura;

    @Min(value = 1, message = "La frecuencia cardíaca debe ser mayor a 0")
    @Max(value = 300, message = "La frecuencia cardiaca no debe superar 300 lpm")
    private Integer frecuenciaCardiaca;

    @Min(value = 1, message = "La frecuencia respiratoria debe ser mayor a 0")
    @Max(value = 200, message = "La frecuencia respiratoria no debe superar 200 rpm")
    private Integer frecuenciaRespiratoria;
    @Size(max = 80, message = "Las mucosas no deben superar 80 caracteres")
    @MeaningfulText(message = "Las mucosas deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las mucosas contienen caracteres no permitidos")
    private String mucosas;
    @Size(max = 80, message = "La turgencia de piel no debe superar 80 caracteres")
    @MeaningfulText(message = "La turgencia de piel debe contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La turgencia de piel contiene caracteres no permitidos")
    private String turgenciaPiel;
    private Boolean vacunacionAlDia;
    private Boolean desparasitacionAlDia;

    private String motivoConsulta;
    @Size(max = 1000, message = "La anamnesis no debe superar 1000 caracteres")
    @MeaningfulText(message = "La anamnesis debe contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La anamnesis contiene caracteres no permitidos")
    private String anamnesis;
    @Size(max = 1000, message = "El examen fisico no debe superar 1000 caracteres")
    @MeaningfulText(message = "El examen fisico debe contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El examen fisico contiene caracteres no permitidos")
    private String examenFisico;
    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    @MeaningfulText(message = "Las observaciones deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las observaciones contienen caracteres no permitidos")
    private String observaciones;

    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String antecedentesEnfermedades;
    @Size(max = 500, message = "Los antecedentes no deben superar 500 caracteres")
    @MeaningfulText(message = "Los antecedentes deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Los antecedentes contienen caracteres no permitidos")
    private String antecedentesProcedimientos;
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

    @Size(max = 500, message = "Las indicaciones no deben superar 500 caracteres")
    @MeaningfulText(message = "Las indicaciones deben contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las indicaciones contienen caracteres no permitidos")
    private String indicacionesReceta;
}
