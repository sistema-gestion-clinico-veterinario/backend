package veterinaria.vargasvet.dto.response;

import java.util.List;

public record VistaPreviaAvisoResponse(int version, String contenido, boolean sinCambios, List<String> observaciones) {
}
