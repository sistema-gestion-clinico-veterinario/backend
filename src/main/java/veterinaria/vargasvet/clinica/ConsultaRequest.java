package veterinaria.vargasvet.clinica;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.shared.TipoConsulta;

@Data
public class ConsultaRequest {

    @NotNull(message = "El campo version es requerido para evitar ediciones simultáneas")
    private Long version;

    private TipoConsulta tipoConsulta;

    private Double pesoEnConsulta;
    private Double temperatura;
    private Integer frecuenciaCardiaca;
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
