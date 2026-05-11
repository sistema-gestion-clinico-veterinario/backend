package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.AdminChangePasswordRequest;
import veterinaria.vargasvet.service.UsuarioService;

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
