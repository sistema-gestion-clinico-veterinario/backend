package veterinaria.vargasvet.dto.request;

import lombok.Data;

@Data
public class ConsultaRequest {

    private Double pesoEnConsulta;
    private Double temperatura;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private String mucosas;
    private String turgenciaPiel;
    private Boolean vacunacionAlDia;
    private Boolean desparasitacionAlDia;
    

    private String anamnesis; 
    private String examenFisico;
    private String observaciones;
    private String motivoConsulta;


    private String antecedentesEnfermedades;
    private String antecedentesProcedimientos;
}
