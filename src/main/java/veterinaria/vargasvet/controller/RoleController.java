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
import veterinaria.vargasvet.dto.response.RolVentanaConfiguracionDTO;
import veterinaria.vargasvet.dto.response.RolMenuOrdenItemDTO;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getAllRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de roles", roleService.getAllRoles()));
    }

    @GetMapping("/company")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
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

    @GetMapping("/company/client-options")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getClientRoleOptions(
            @RequestParam(required = false) Integer companyId) {
        Integer effectiveId = resolveCompanyId(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles de cliente asignables",
                roleService.getAssignableRoles(effectiveId, RoleScope.CLIENT)));
    }

    @GetMapping("/company/staff-options")
    @PreAuthorize("@accesoValidator.can('VISTA_EMPLEADOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getStaffRoleOptions(
            @RequestParam(required = false) Integer companyId) {
        Integer effectiveId = resolveCompanyId(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles de personal asignables",
                roleService.getAssignableRoles(effectiveId, RoleScope.STAFF)));
    }

    @GetMapping("/system")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getSystemRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Roles del sistema", roleService.getSystemRoles()));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<RolDTO>> createRole(@Valid @RequestBody RoleRequest body) {
        String nombre      = body.getName() != null ? body.getName() : body.getNombre();
        String descripcion = body.getDescripcion();
        Integer companyId  = SecurityUtils.isSuperAdmin()
                ? body.getCompanyId()
                : SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Rol creado",
                        roleService.createRole(nombre, descripcion, companyId, body.getScope())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<RolDTO>> updateRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleRequest body) {
        String nombre      = body.getName() != null ? body.getName() : body.getNombre();
        String descripcion = body.getDescripcion();
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol actualizado",
                roleService.updateRole(id, nombre, descripcion, body.getScope())));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<RolDTO>> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado del rol actualizado", roleService.toggleActivo(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol eliminado", null));
    }

    @GetMapping("/{id}/views")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolVistaPermisoDTO>>> getVistas(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vistas del rol",
                roleService.getVistasByRole(id)));
    }

    @PutMapping("/{id}/views")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<List<RolVistaPermisoDTO>>> saveVistas(
            @PathVariable Integer id,
            @RequestBody List<RolVistaPermisoDTO> permisos) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Permisos guardados",
                roleService.saveVistasByRole(id, permisos)));
    }

    @GetMapping("/{id}/menu-configuration")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolVentanaConfiguracionDTO>>> getMenuConfiguration(
            @PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Organización del menú",
                roleService.getMenuConfiguration(id)));
    }

    @PutMapping("/{id}/menu-configuration")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<List<RolVentanaConfiguracionDTO>>> saveMenuConfiguration(
            @PathVariable Integer id,
            @RequestBody List<RolVentanaConfiguracionDTO> configuraciones) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Organización del menú guardada",
                roleService.saveMenuConfiguration(id, configuraciones)));
    }

    @GetMapping("/{id}/menu-order")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'LEER')")
    public ResponseEntity<ApiResponse<List<RolMenuOrdenItemDTO>>> getMenuOrder(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Orden del menú", roleService.getMenuOrder(id)));
    }

    @PutMapping("/{id}/menu-order")
    @PreAuthorize("@accesoValidator.can('VISTA_ROLES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<List<RolMenuOrdenItemDTO>>> saveMenuOrder(
            @PathVariable Integer id,
            @RequestBody List<RolMenuOrdenItemDTO> items) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Orden del menú guardado",
                roleService.saveMenuOrder(id, items)));
    }

    private Integer resolveCompanyId(Integer companyId) {
        Integer effectiveId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();
        if (effectiveId == null) {
            throw new IllegalArgumentException("Debe indicar el companyId");
        }
        return effectiveId;
    }
}
