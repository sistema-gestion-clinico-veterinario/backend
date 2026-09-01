package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;


@Component
@RequiredArgsConstructor
public class AccesoValidator {

    private final RolePermissionEvaluator rolePermissionEvaluator;

    public void validarLeer(String codigoVista) {
        if (!can(codigoVista, "LEER"))
            throw new AccessDeniedException("Sin acceso de lectura a: " + codigoVista);
    }

    public void validarEscribir(String codigoVista) {
        if (!can(codigoVista, "ESCRIBIR"))
            throw new AccessDeniedException("Sin acceso de escritura a: " + codigoVista);
    }

    public void validarModificar(String codigoVista) {
        if (!can(codigoVista, "MODIFICAR"))
            throw new AccessDeniedException("Sin acceso de modificación a: " + codigoVista);
    }

    public void validarEliminar(String codigoVista) {
        if (!can(codigoVista, "ELIMINAR"))
            throw new AccessDeniedException("Sin acceso de eliminación a: " + codigoVista);
    }

    public boolean puedeLeer(String codigoVista) {
        return can(codigoVista, "LEER");
    }

    public boolean hasPurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) return false;
        try {
            return SecurityUtils.getCurrentRolePurpose()
                    == RolePurpose.valueOf(purpose.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean hasScope(String scope) {
        if (scope == null || scope.isBlank()) return false;
        try {
            return SecurityUtils.getCurrentRoleScope()
                    == RoleScope.valueOf(scope.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean can(String codigoVista, String accion) {
        if (codigoVista == null || codigoVista.isBlank() || accion == null || accion.isBlank()) {
            return false;
        }

        return rolePermissionEvaluator.can(SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentRoleId(),
                codigoVista, accion);
    }

    public boolean canAccessCompanyData(String codigoVista) {
        return rolePermissionEvaluator.dataScope(SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentRoleId(),
                codigoVista) == veterinaria.vargasvet.domain.enums.DataScope.COMPANY;
    }
}
