package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.DecidirConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.request.RevocarConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.response.ConsentimientoVeterinarioResponse;
import veterinaria.vargasvet.service.ConsentimientoVeterinarioService;
import veterinaria.vargasvet.security.ClientIpResolver;

import java.util.List;

@RestController
@RequestMapping("/me/consentimientos-veterinarios")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ConsentimientoVeterinarioTitularController {
    private final ConsentimientoVeterinarioService service;
    private final ClientIpResolver clientIpResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsentimientoVeterinarioResponse>>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Mis consentimientos veterinarios", service.listarMios()));
    }

    @PostMapping("/{uuid}/decision")
    public ResponseEntity<ApiResponse<ConsentimientoVeterinarioResponse>> decidir(
            @PathVariable String uuid, @Valid @RequestBody DecidirConsentimientoVeterinarioRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Decisión registrada",
                service.decidir(uuid, request, clientIpResolver.resolve(http), http.getHeader("User-Agent"))));
    }

    @PostMapping("/{uuid}/revocar")
    public ResponseEntity<ApiResponse<ConsentimientoVeterinarioResponse>> revocar(
            @PathVariable String uuid, @Valid @RequestBody RevocarConsentimientoVeterinarioRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consentimiento revocado", service.revocarMio(uuid, request.motivo())));
    }
}
