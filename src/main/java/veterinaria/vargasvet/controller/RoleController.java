package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.RoleRequest;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getAllRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de roles", roleService.getAllRoles()));
    }

    @GetMapping("/company")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getRolesByCompany(
            @RequestParam(required = false) Integer companyId) {

        Integer effectiveId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();

        if (effectiveId == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Debe indicar el companyId", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles de empresa",
                roleService.getRolesByCompany(effectiveId)));
    }

    @GetMapping("/system")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getSystemRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles del sistema", roleService.getSystemRoles()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RolDTO>> createRole(@Valid @RequestBody RoleRequest body) {
        String nombre      = body.getName() != null ? body.getName() : body.getNombre();
        String descripcion = body.getDescripcion();
        Integer companyId  = SecurityUtils.isSuperAdmin()
                ? body.getCompanyId()
                : SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Rol creado", roleService.createRole(nombre, descripcion, companyId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RolDTO>> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleRequest body) {
        String nombre      = body.getName() != null ? body.getName() : body.getNombre();
        String descripcion = body.getDescripcion();
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol actualizado",
                roleService.updateRole(id, nombre, descripcion)));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RolDTO>> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado del rol actualizado", roleService.toggleActivo(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol eliminado", null));
    }

    @GetMapping("/{id}/views")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolVistaPermisoDTO>>> getVistas(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vistas del rol",
                roleService.getVistasByRole(id)));
    }

    @PutMapping("/{id}/views")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolVistaPermisoDTO>>> saveVistas(
            @PathVariable Integer id,
            @RequestBody List<RolVistaPermisoDTO> permisos) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Permisos guardados",
                roleService.saveVistasByRole(id, permisos)));
    }
}
