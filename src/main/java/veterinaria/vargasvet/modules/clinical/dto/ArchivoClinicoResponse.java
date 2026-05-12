package veterinaria.vargasvet.modules.clinical.dto;

import lombok.Data;
import veterinaria.vargasvet.modules.clinical.domain.enums.TipoArchivo;

import java.time.LocalDateTime;

@Data
public class ArchivoClinicoResponse {
    private Long id;
    private String nombre;
    private TipoArchivo tipo;
    private String tipoMime;
    private Long tamanioBytes;
    private String url;
    private String descripcion;
    private String subidoPor;
    private LocalDateTime fechaCarga;
}
