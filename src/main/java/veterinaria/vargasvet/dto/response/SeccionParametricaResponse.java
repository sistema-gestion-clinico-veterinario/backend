package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SeccionParametricaResponse {

    private String analizador;
    private List<ParametroClinicoResponse> parametros;
}
