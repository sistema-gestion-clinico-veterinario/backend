package veterinaria.vargasvet.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.auth.AdminChangePasswordRequest;
import veterinaria.vargasvet.admin.UsuarioService;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'USER_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody AdminChangePasswordRequest request) {
        usuarioService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña restablecida exitosamente", null));
    }
}
