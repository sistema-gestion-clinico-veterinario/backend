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
    private final veterinaria.vargasvet.service.AutorizacionIaService autorizacionIaService;

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@accesoValidator.can('VISTA_LABORATORIO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<LaboratorioIAResponse>> analizarLaboratorio(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("mascotaId") Long mascotaId,
            @RequestParam(value = "especie", defaultValue = "Perro") String especie) {

        var autorizacion = autorizacionIaService.exigir(mascotaId);
        LaboratorioIAResponse resultado = laboratorioIAService.analizarLaboratorio(archivo, especie);
        autorizacionIaService.registrarUso(mascotaId, "laboratorio", autorizacion);
        return ResponseEntity.ok(new ApiResponse<>(true, "Laboratorio analizado exitosamente", resultado));
    }
}
