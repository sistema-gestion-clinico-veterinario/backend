package veterinaria.vargasvet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParametroClinicoResponse {

    private String test;
    private Double valor;
    private String unidad;

    @JsonProperty("ref_min")
    private Double refMin;

    @JsonProperty("ref_max")
    private Double refMax;

    private String flag;
    private String estado;
}
