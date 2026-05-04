package veterinaria.vargasvet.service;

import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.domain.enums.TipoArchivo;
import veterinaria.vargasvet.dto.response.ArchivoClinicoResponse;

import java.util.List;

public interface ArchivoClinicoService {
    ArchivoClinicoResponse subirArchivo(Long consultaId, MultipartFile file, TipoArchivo tipo, String descripcion);
    List<ArchivoClinicoResponse> listarPorConsulta(Long consultaId);
}
