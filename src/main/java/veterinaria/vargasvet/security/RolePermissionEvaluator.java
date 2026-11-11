package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.domain.enums.DataScope;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RolePermissionEvaluator {

    public static final String ASIGNACION_ACTIVA_ATTR = RolePermissionEvaluator.class.getName() + ".asignacionActiva";

    public record AsignacionDeSolicitud(Integer userId, Integer roleId, Integer companyId, UsuarioPorRol asignacion) { }

    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;

    private UsuarioPorRol asignacionActiva(Integer userId, Integer roleId, Integer assignmentCompanyId) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                && atributos.getRequest().getAttribute(ASIGNACION_ACTIVA_ATTR) instanceof AsignacionDeSolicitud cargada
                && Objects.equals(cargada.userId(), userId)
                && Objects.equals(cargada.roleId(), roleId)
                && Objects.equals(cargada.companyId(), assignmentCompanyId)) {
            return cargada.asignacion();
        }
        return usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(userId, roleId, assignmentCompanyId)
                .orElse(null);
    }

    public boolean can(Integer userId, Integer roleId, Integer assignmentCompanyId,
                       String viewCode, String action) {
        if (userId == null || roleId == null || viewCode == null || viewCode.isBlank()
                || action == null || action.isBlank()) return false;

        UsuarioPorRol assignment = asignacionActiva(userId, roleId, assignmentCompanyId);
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

    public DataScope dataScope(Integer userId, Integer roleId, Integer assignmentCompanyId, String viewCode) {
        if (userId == null || roleId == null || viewCode == null || viewCode.isBlank()) return DataScope.OWN;
        UsuarioPorRol assignment = asignacionActiva(userId, roleId, assignmentCompanyId);
        if (assignment == null) return DataScope.OWN;
        if (assignment.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN
                || assignment.getRol().getPurpose() == RolePurpose.COMPANY_ADMIN) {
            return DataScope.COMPANY;
        }
        return rolVistaPermisoRepository.findByRolIdAndVistaCodigo(roleId, viewCode)
                .filter(permission -> permission.getVista().isActivo())
                .filter(permission -> permission.isLeer())
                .filter(permission -> isAudienceCompatible(
                        assignment.getRol().getScope(), permission.getVista().getAudience()))
                .map(permission -> permission.getDataScope() != null ? permission.getDataScope() : DataScope.OWN)
                .orElse(DataScope.OWN);
    }

    private boolean isAudienceCompatible(RoleScope scope, ViewAudience audience) {
        if (scope == RoleScope.PLATFORM || audience == ViewAudience.SHARED) return true;
        return (scope == RoleScope.STAFF && audience == ViewAudience.STAFF)
                || (scope == RoleScope.CLIENT && audience == ViewAudience.CLIENT);
    }
}
