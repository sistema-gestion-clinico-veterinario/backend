package veterinaria.vargasvet.modules.clinical.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsultaResumenResponse {
    private Long id;
    private LocalDateTime fechaConsulta;
    private String motivoConsulta;
    private String tipoConsulta;
    private String estado;
    private String veterinarioNombre;

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

    private List<DiagnosticoResumenResponse> diagnosticos;
    private List<TratamientoResumenResponse> tratamientos;
    private List<PrescripcionResumenResponse> prescripciones;
    private List<ArchivoClinicoResponse> archivos;
}
