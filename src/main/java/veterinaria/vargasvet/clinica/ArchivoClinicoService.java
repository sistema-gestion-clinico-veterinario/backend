package veterinaria.vargasvet.clinica;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.shared.TipoArchivo;
import veterinaria.vargasvet.clinica.ArchivoClinicoResponse;

import java.util.List;

public interface ArchivoClinicoService {
    ArchivoClinicoResponse subirArchivo(Long consultaId, MultipartFile file, TipoArchivo tipo, String descripcion);
    List<ArchivoClinicoResponse> listarPorConsulta(Long consultaId);
    Resource servirContenido(Long id);
    ArchivoClinicoResponse obtenerPorId(Long id);
    void eliminar(Long id);
}
