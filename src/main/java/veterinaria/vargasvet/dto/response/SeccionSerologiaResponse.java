package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SeccionSerologiaResponse {

    private String analizador;
    private List<ParametroSerologicoResponse> parametros;
}
