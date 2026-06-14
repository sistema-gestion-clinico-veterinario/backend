package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;
import veterinaria.vargasvet.service.RadiografiaIAService;

@RestController
@RequestMapping("/radiografia")
@RequiredArgsConstructor
public class RadiografiaIAController {

    private final RadiografiaIAService radiografiaIAService;

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<RadiografiaPrediccionResponse>> analizarRadiografia(
            @RequestParam("file") MultipartFile file) {
        RadiografiaPrediccionResponse resultado = radiografiaIAService.analizarRadiografia(file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Radiografía analizada exitosamente", resultado));
    }
}
