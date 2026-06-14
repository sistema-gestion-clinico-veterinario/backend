package veterinaria.vargasvet.service;

import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;

public interface RadiografiaIAService {
    RadiografiaPrediccionResponse analizarRadiografia(MultipartFile file);
}
