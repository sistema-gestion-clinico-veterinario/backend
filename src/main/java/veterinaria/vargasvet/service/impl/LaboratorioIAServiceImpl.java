package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.integration.LaboratorioIAClient;
import veterinaria.vargasvet.service.LaboratorioIAService;

@Service
@RequiredArgsConstructor
public class LaboratorioIAServiceImpl implements LaboratorioIAService {

    private final LaboratorioIAClient laboratorioIAClient;

    private static final java.util.Set<String> EXTENSIONES_VALIDAS = java.util.Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".bmp", ".tiff"
    );

    @Override
    public LaboratorioIAResponse analizarLaboratorio(MultipartFile archivo, String especie) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }

        String filename = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase()
                : "";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";

        if (!EXTENSIONES_VALIDAS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Formato no soportado: \"" + ext + "\". Use PDF, JPG, PNG, BMP o TIFF.");
        }

        try {
            return laboratorioIAClient.analizar(archivo, especie);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al conectar con el servicio de análisis de laboratorio: " + e.getMessage());
        }
    }
}
