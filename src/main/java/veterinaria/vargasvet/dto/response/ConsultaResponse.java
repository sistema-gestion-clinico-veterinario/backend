package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsultaResponse {

    private Long id;
    private Long citaId;
    private Long historiaClinicaId;
    private String numeroHc;
    private Long mascotaId;
    private String mascotaNombre;
    private String especie;
    private String raza;
    private String sexo;
    private String color;
    private String senasParticulares;
    private Integer edadAproximadaMeses; 
    
    private Long apoderadoId;
    private String apoderadoNombre;
    private String apoderadoTelefono;
    private String apoderadoDireccion;


    private Long veterinarioId;
    private String veterinarioNombre;


    private LocalDateTime fechaConsulta;
    private String motivoConsulta;
    private String anamnesis;
    private String examenFisico;
    private Double pesoEnConsulta;
    private Double temperatura;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private String mucosas;
    private String turgenciaPiel;
    private Boolean vacunacionAlDia;
    private Boolean desparasitacionAlDia;
    private String observaciones;


    private String antecedentesEnfermedades;
    private String antecedentesProcedimientos;
}
