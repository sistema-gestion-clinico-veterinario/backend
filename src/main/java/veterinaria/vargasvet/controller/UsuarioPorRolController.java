package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.service.UsuarioPorRolService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsuarioPorRolController {

    private final UsuarioPorRolService usuarioPorRolService;
    private final MenuBuilderService menuBuilderService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/{userId}/roles")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<UsuarioPorRol>>> listarRoles(@PathVariable("userId") Integer usuarioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles del usuario",
                usuarioPorRolService.listarPorUsuario(usuarioId)));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<UsuarioPorRol>> asignarRol(
            @PathVariable("userId") Integer usuarioId,
            @PathVariable("roleId") Integer rolId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol asignado",
                usuarioPorRolService.asignarRol(usuarioId, rolId)));
    }

    @DeleteMapping("/roles/{userRoleId}")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> revocarRol(@PathVariable("userRoleId") Integer usuarioPorRolId) {
        usuarioPorRolService.revocarRol(usuarioPorRolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol revocado", null));
    }

    @GetMapping("/me/menu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> miMenu() {

        String email = SecurityUtils.getCurrentUserEmail();
        Integer usuarioId = usuarioRepository.findByEmail(email)
                .orElseThrow().getId();

        List<MenuItemDTO> menu = menuBuilderService.construirMenu(usuarioId, SecurityUtils.getCurrentRoleId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú del usuario", menu));
    }
}
