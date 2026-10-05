package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.Objects;

/**
 * Reglas que evitan dejar a una empresa sin administración: solo un administrador gestiona la
 * cuenta de otro administrador, y nunca se desactiva al único administrador activo, tampoco por
 * la plataforma. La plataforma (SuperAdmin) solo queda fuera de la regla de quién puede gestionar,
 * porque es quien asigna la administración de una empresa.
 */
@Component
@RequiredArgsConstructor
public class AdministratorProtection {

    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final CompanyMembershipService companyMembershipService;
    private final CompanyRepository companyRepository;

    public boolean isAdministrator(Usuario usuario, Integer companyId) {
        return usuarioPorRolRepository.findByUsuarioId(usuario.getId()).stream()
                .anyMatch(assignment -> assignment.getCompany() != null
                        && Objects.equals(assignment.getCompany().getId(), companyId)
                        && assignment.getRol() != null
                        && assignment.getRol().isActivo()
                        && assignment.getRol().getPurpose() == RolePurpose.COMPANY_ADMIN);
    }

    public void assertCanManage(Usuario target, Integer companyId) {
        if (SecurityUtils.isSuperAdmin()) return;
        if (isAdministrator(target, companyId) && !SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Solo un administrador puede gestionar la cuenta de otro administrador");
        }
    }

    public void assertNotLastAdministrator(Usuario target, Integer companyId) {
        if (!isAdministrator(target, companyId)) return;
        if (!anotherAdministratorRemains(target, companyId)) {
            throw new IllegalStateException(
                    "No puedes cerrar tu cuenta porque eres el único administrador activo de la empresa. "
                            + "Asigna primero a otra persona como administradora.");
        }
    }

    public void assertCanDeactivate(Usuario target, Integer companyId) {
        assertAnotherAdministratorRemains(target, companyId, "desactivar al");
    }

    /** Quitarle el rol de administrador a alguien equivale, para la empresa, a perder a ese administrador. */
    public void assertCanRemoveAdministratorRole(Usuario target, Integer companyId) {
        assertAnotherAdministratorRemains(target, companyId, "quitar el rol de administrador al");
    }

    private void assertAnotherAdministratorRemains(Usuario target, Integer companyId, String accion) {
        assertCanManage(target, companyId);
        if (!isAdministrator(target, companyId)) return;
        if (!anotherAdministratorRemains(target, companyId)) {
            throw new IllegalStateException(
                    "No se puede " + accion + " único administrador activo de la empresa. "
                            + "Asigna primero a otra persona como administradora.");
        }
    }

    /** Un reemplazo debe poder ejercer: cuenta activada y empleado activo de la empresa. Un cliente, una persona
     * pendiente o un administrador de otra empresa no cuentan. La fila de la empresa se bloquea antes de contar para
     * que dos administradores que se desactivan entre sí no vean cada uno al otro como el reemplazo. */
    private boolean anotherAdministratorRemains(Usuario target, Integer companyId) {
        companyRepository.lockById(companyId);
        return usuarioPorRolRepository
                .findUsersWithActiveRolePurpose(companyId, RolePurpose.COMPANY_ADMIN).stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), target.getId()))
                .filter(Usuario::isActivo)
                .anyMatch(candidate -> companyMembershipService.hasActiveStaffMembership(candidate.getId(), companyId));
    }
}
