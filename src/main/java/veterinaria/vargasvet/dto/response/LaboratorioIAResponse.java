package veterinaria.vargasvet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LaboratorioIAResponse {

    private String fuente;
    private String tipo;
    private String especie;
    private String raza;
    private String edad;
    private String fecha;

    @JsonProperty("secciones_presentes")
    private List<String> seccionesPresentes;

    @JsonProperty("comentarios_clinicos")
    private List<String> comentariosClinicos;

    private SeccionParametricaResponse hematologia;
    private SeccionParametricaResponse quimica;
    private SeccionSerologiaResponse serologia;

    private List<String> alertas;
}
