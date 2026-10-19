package veterinaria.vargasvet.dto.response;

import java.util.List;

public record CentroLegalPublicoResponse(String clinica, String logoUrl, String colorPrimario,
                                         List<DocumentoCentroLegalResponse> documentos) {
}
