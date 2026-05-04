package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class HistoriaClinicaDetalleResponse {
    private Long id;
    private String numeroHc;
    private Boolean activa;

    private Long mascotaId;
    private String mascotaNombre;
    private String especie;
    private String raza;
    private String sexo;
    private String color;
    private String senasParticulares;
    private Integer edadAproximadaMeses;
    private Boolean esterilizado;

    private Long apoderadoId;
    private String propietarioNombre;
    private String propietarioTelefono;
    private String propietarioDireccion;

    private String enfermedades;
    private String procedimientos;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String grupoSanguineo;

    private List<ConsultaResumenResponse> consultas;
}
