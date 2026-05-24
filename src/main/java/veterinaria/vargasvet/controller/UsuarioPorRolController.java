package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.UsuarioPorRolPermiso;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.service.UsuarioPorRolService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsuarioPorRolController {

    private final UsuarioPorRolService usuarioPorRolService;
    private final MenuBuilderService menuBuilderService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<List<UsuarioPorRol>>> listarRoles(@PathVariable("userId") Integer usuarioId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles del usuario",
                usuarioPorRolService.listarPorUsuario(usuarioId)));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<UsuarioPorRol>> asignarRol(
            @PathVariable("userId") Integer usuarioId,
            @PathVariable("roleId") Integer rolId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol asignado",
                usuarioPorRolService.asignarRol(usuarioId, rolId)));
    }

    @DeleteMapping("/roles/{userRoleId}")
    public ResponseEntity<ApiResponse<Void>> revocarRol(@PathVariable("userRoleId") Integer usuarioPorRolId) {
        usuarioPorRolService.revocarRol(usuarioPorRolId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol revocado", null));
    }

    @PostMapping("/roles/{userRoleId}/permissions")
    public ResponseEntity<ApiResponse<UsuarioPorRolPermiso>> asignarPermiso(
            @PathVariable("userRoleId") Integer usuarioPorRolId,
            @RequestBody Map<String, Object> body) {

        Integer ventanaId = (Integer) body.get("ventanaId");
        boolean leer     = Boolean.TRUE.equals(body.get("leer"));
        boolean escribir = Boolean.TRUE.equals(body.get("escribir"));
        boolean modificar = Boolean.TRUE.equals(body.get("modificar"));
        boolean eliminar  = Boolean.TRUE.equals(body.get("eliminar"));

        UsuarioPorRolPermiso permiso = usuarioPorRolService
                .asignarPermiso(usuarioPorRolId, ventanaId, leer, escribir, modificar, eliminar);

        return ResponseEntity.ok(new ApiResponse<>(true, "Permiso asignado", permiso));
    }

    @GetMapping("/me/menu")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> miMenu(
            @RequestParam(required = false) String rol) {

        String email = SecurityUtils.getCurrentUserEmail();
        Integer usuarioId = usuarioRepository.findByEmail(email)
                .orElseThrow().getId();

        List<MenuItemDTO> menu = menuBuilderService.construirMenu(usuarioId, rol);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú del usuario", menu));
    }
}
