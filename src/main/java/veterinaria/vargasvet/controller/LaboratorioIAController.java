package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.service.LaboratorioIAService;

@RestController
@RequestMapping("/laboratorio")
@RequiredArgsConstructor
public class LaboratorioIAController {

    private final LaboratorioIAService laboratorioIAService;

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<LaboratorioIAResponse>> analizarLaboratorio(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "especie", defaultValue = "Perro") String especie) {

        LaboratorioIAResponse resultado = laboratorioIAService.analizarLaboratorio(archivo, especie);
        return ResponseEntity.ok(new ApiResponse<>(true, "Laboratorio analizado exitosamente", resultado));
    }
}
