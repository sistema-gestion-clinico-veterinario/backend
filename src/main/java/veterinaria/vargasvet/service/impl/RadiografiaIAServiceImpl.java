package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;
import veterinaria.vargasvet.integration.RadiografiaIAClient;
import veterinaria.vargasvet.service.RadiografiaIAService;

@Service
@RequiredArgsConstructor
public class RadiografiaIAServiceImpl implements RadiografiaIAService {

    private final RadiografiaIAClient radiografiaIAClient;

    @Override
    public RadiografiaPrediccionResponse analizarRadiografia(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de radiografía no puede estar vacío.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean validExtension = filename.endsWith(".dcm") || filename.endsWith(".dicom")
                || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");

        if (!validExtension) {
            throw new IllegalArgumentException("Formato no soportado. Use DICOM (.dcm), PNG o JPG.");
        }

        try {
            return radiografiaIAClient.predict(file);
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con el servicio de análisis de radiografías: " + e.getMessage());
        }
    }
}
