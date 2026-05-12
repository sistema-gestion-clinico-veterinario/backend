package veterinaria.vargasvet.clinica;

import lombok.Data;
import veterinaria.vargasvet.shared.TipoArchivo;

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
