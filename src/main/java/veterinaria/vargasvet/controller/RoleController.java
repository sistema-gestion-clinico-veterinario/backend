package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.dto.response.PermissionDTO;
import veterinaria.vargasvet.dto.response.RoleDTO;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de roles obtenida", roleService.getAllRoles()));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllPermissions() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de permisos obtenida", roleService.getAllPermissions()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Rol creado exitosamente", roleService.createRole(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(@PathVariable Integer id, @Valid @RequestBody RoleCreateDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol actualizado exitosamente", roleService.updateRole(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol eliminado exitosamente", null));
    }
}
