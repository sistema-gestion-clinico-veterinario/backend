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
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    /** Todos los roles (solo SUPER_ADMIN) */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de roles obtenida", roleService.getAllRoles()));
    }

    /** Roles de una empresa.
     *  - Admin normal: usa su propio companyId del token (no necesita enviar param).
     *  - SUPER_ADMIN: debe enviar ?companyId=X porque no tiene empresa en el token.
     */
    @GetMapping("/empresa")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getRolesByCompany(
            @RequestParam(required = false) Integer companyId) {

        Integer effectiveCompanyId;
        if (SecurityUtils.isSuperAdmin()) {
            if (companyId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "El Super Admin debe indicar el companyId", null));
            }
            effectiveCompanyId = companyId;
        } else {
            effectiveCompanyId = SecurityUtils.getCurrentCompanyId();
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Roles de empresa obtenidos",
                roleService.getRolesByCompany(effectiveCompanyId)));
    }

    /** Roles del sistema (company_id IS NULL) — Solo SUPER_ADMIN */
    @GetMapping("/sistema")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getSystemRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles del sistema obtenidos",
                roleService.getSystemRoles()));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllPermissions() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de permisos obtenida", roleService.getAllPermissions()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        // Si no es SUPER_ADMIN, forzar el companyId del usuario actual
        if (!SecurityUtils.isSuperAdmin()) {
            dto.setCompanyId(SecurityUtils.getCurrentCompanyId());
        }
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
