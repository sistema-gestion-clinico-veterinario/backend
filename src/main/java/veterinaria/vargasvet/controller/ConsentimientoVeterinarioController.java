package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoConsentimientoVeterinario;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CrearConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.request.RevocarConsentimientoVeterinarioRequest;
import veterinaria.vargasvet.dto.response.ConsentimientoVeterinarioResponse;
import veterinaria.vargasvet.service.ConsentimientoVeterinarioService;

import java.util.List;

@RestController
@RequestMapping("/consentimientos-veterinarios")
@RequiredArgsConstructor
public class ConsentimientoVeterinarioController {
    private final ConsentimientoVeterinarioService service;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<ConsentimientoVeterinarioResponse>>> listar(
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) EstadoConsentimientoVeterinario estado) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consentimientos veterinarios", service.listar(mascotaId, estado)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<ConsentimientoVeterinarioResponse>> crear(
            @Valid @RequestBody CrearConsentimientoVeterinarioRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consentimiento enviado para decisión", service.crear(request)));
    }

    @PostMapping("/{uuid}/revocar")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ConsentimientoVeterinarioResponse>> revocar(
            @PathVariable String uuid, @Valid @RequestBody RevocarConsentimientoVeterinarioRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Consentimiento revocado", service.revocarPorPersonal(uuid, request.motivo())));
    }
}
