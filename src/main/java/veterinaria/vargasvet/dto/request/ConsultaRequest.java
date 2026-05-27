package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoConsulta;

@Data
public class ConsultaRequest {

    @NotNull(message = "El campo version es requerido para evitar ediciones simultáneas")
    private Long version;

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
    private String mucosas;
    private String turgenciaPiel;
    private Boolean vacunacionAlDia;
    private Boolean desparasitacionAlDia;

    private String motivoConsulta;
    private String anamnesis;
    private String examenFisico;
    private String observaciones;

    private String antecedentesEnfermedades;
    private String antecedentesProcedimientos;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String grupoSanguineo;

    private String indicacionesReceta;
}
