package veterinaria.vargasvet.service;

import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;

public interface LaboratorioIAService {
    LaboratorioIAResponse analizarLaboratorio(MultipartFile archivo, String especie);
}
