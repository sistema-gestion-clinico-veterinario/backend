package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.AdminPasswordResetRequest;
import veterinaria.vargasvet.service.UsuarioService;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/reset-password")
    @PreAuthorize("@accesoValidator.can('VISTA_GESTION_CREDENCIALES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody AdminPasswordResetRequest request) {
        usuarioService.requestPasswordReset(request);
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Se enviaron al usuario las instrucciones para restablecer su acceso", null));
    }
}
