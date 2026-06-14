package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class ParametroSerologicoResponse {

    private String test;
    private String resultado;
    private Boolean positivo;
}
