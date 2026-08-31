package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RolePermissionEvaluator {

    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;

    public boolean can(Integer userId, Integer roleId, String viewCode, String action) {
        if (userId == null || roleId == null || viewCode == null || viewCode.isBlank()
                || action == null || action.isBlank()) return false;

        UsuarioPorRol assignment = usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleId(userId, roleId)
                .orElse(null);
        if (assignment == null) return false;
        if (assignment.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN) return true;

        return rolVistaPermisoRepository.findByRolIdAndVistaCodigo(roleId, viewCode)
                .filter(permission -> permission.getVista().isActivo())
                .filter(permission -> isAudienceCompatible(
                        assignment.getRol().getScope(), permission.getVista().getAudience()))
                .map(permission -> switch (action.trim().toUpperCase(Locale.ROOT)) {
                    case "LEER", "READ" -> permission.isLeer();
                    case "ESCRIBIR", "CREAR", "CREATE", "WRITE" -> permission.isEscribir();
                    case "MODIFICAR", "EDITAR", "UPDATE", "EDIT" -> permission.isModificar();
                    case "ELIMINAR", "DELETE" -> permission.isEliminar();
                    default -> false;
                })
                .orElse(false);
    }

    private boolean isAudienceCompatible(RoleScope scope, ViewAudience audience) {
        if (scope == RoleScope.PLATFORM || audience == ViewAudience.SHARED) return true;
        return (scope == RoleScope.STAFF && audience == ViewAudience.STAFF)
                || (scope == RoleScope.CLIENT && audience == ViewAudience.CLIENT);
    }
}
