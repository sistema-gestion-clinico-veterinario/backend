package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.SugerenciaControlResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.SeguimientoClinicoService;

import java.util.List;

@RestController
@RequestMapping("/clinical-follow-up")
@RequiredArgsConstructor
public class SeguimientoClinicoController {

    private final SeguimientoClinicoService seguimientoClinicoService;
    private final AccesoValidator accesoValidator;

    @GetMapping("/suggestions")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<SugerenciaControlResponse>>> listarSugerencias(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "7") int dias) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Sugerencias de control obtenidas",
                seguimientoClinicoService.listarSugerenciasControl(companyId, dias)));
    }
}
