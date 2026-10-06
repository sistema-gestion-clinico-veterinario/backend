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
    private final veterinaria.vargasvet.service.AutorizacionIaService autorizacionIaService;

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@accesoValidator.can('VISTA_LABORATORIO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<RadiografiaPrediccionResponse>> analizarRadiografia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mascotaId") Long mascotaId) {
        var autorizacion = autorizacionIaService.exigir(mascotaId);
        RadiografiaPrediccionResponse resultado = radiografiaIAService.analizarRadiografia(file);
        autorizacionIaService.registrarUso(mascotaId, "radiografía", autorizacion);
        return ResponseEntity.ok(new ApiResponse<>(true, "Radiografía analizada exitosamente", resultado));
    }
}
