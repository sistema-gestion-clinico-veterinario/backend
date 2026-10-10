package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Único punto de escritura de las asignaciones de roles de una empresa.
 *
 * <p>Editar los datos de una persona y concederle privilegios son operaciones
 * distintas. Este servicio aplica la autorización de RBAC, el aislamiento por
 * empresa y las reglas de protección del administrador antes de reemplazar las
 * asignaciones.</p>
 */
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private static final String ROLES_VIEW = "VISTA_ROLES";

    private final UsuarioPorRolRepository assignmentRepository;
    private final RoleRepository roleRepository;
    private final AccesoValidator accesoValidator;
    private final AdministratorProtection administratorProtection;
    private final AuditLogService auditLogService;

    @Transactional
    public void replaceStaffRoles(Usuario usuario, Company company, Set<Integer> requestedRoleIds) {
        replaceCompanyRoles(usuario, company, RoleScope.STAFF, requestedRoleIds, false);
    }

    @Transactional
    public void replaceClientRoles(Usuario usuario, Company company, Set<Integer> requestedRoleIds) {
        replaceCompanyRoles(usuario, company, RoleScope.CLIENT, requestedRoleIds, true);
    }

    private void replaceCompanyRoles(Usuario usuario,
                                     Company company,
                                     RoleScope expectedScope,
                                     Set<Integer> requestedRoleIds,
                                     boolean allowInitialClientPortalRole) {
        requireCompanyContext(company);
        if (requestedRoleIds == null || requestedRoleIds.isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un rol");
        }

        List<UsuarioPorRol> currentAssignments = assignmentRepository
                .findByUsuarioIdAndCompanyIdAndRoleScope(usuario.getId(), company.getId(), expectedScope);
        Map<Integer, Role> currentRoles = currentAssignments.stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getRol().getId(),
                        UsuarioPorRol::getRol,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        Set<Integer> requestedIds = new LinkedHashSet<>(requestedRoleIds);
        if (requestedIds.equals(currentRoles.keySet())) {
            return;
        }

        List<Role> requestedRoles = loadAndValidateRoles(requestedIds, company, expectedScope, currentRoles);
        boolean initialBaseClientRole = allowInitialClientPortalRole
                && currentRoles.isEmpty()
                && requestedRoles.size() == 1
                && requestedRoles.getFirst().getPurpose() == RolePurpose.CLIENT_PORTAL
                && requestedRoles.getFirst().isSystemManaged();
        if (!initialBaseClientRole) {
            accesoValidator.validarModificar(ROLES_VIEW);
        }

        boolean removesCompanyAdmin = currentRoles.values().stream()
                .anyMatch(role -> role.getPurpose() == RolePurpose.COMPANY_ADMIN)
                && requestedRoles.stream().noneMatch(role -> role.getPurpose() == RolePurpose.COMPANY_ADMIN);
        if (removesCompanyAdmin) {
            administratorProtection.assertCanRemoveAdministratorRole(usuario, company.getId());
        }

        Set<String> previousNames = roleNames(currentRoles.values());
        Set<String> requestedNames = roleNames(requestedRoles);

        assignmentRepository.deleteByUsuarioIdAndCompanyIdAndRoleScope(
                usuario.getId(), company.getId(), expectedScope);
        for (Role role : requestedRoles) {
            UsuarioPorRol assignment = new UsuarioPorRol();
            assignment.setUsuario(usuario);
            assignment.setRol(role);
            assignment.setCompany(company);
            assignmentRepository.save(assignment);
        }

        auditLogService.log(
                company.getId(),
                "MODIFICAR_ASIGNACIONES_ROL",
                "Roles",
                "Usuario " + usuario.getId() + ": roles " + previousNames + " -> " + requestedNames);
    }

    private List<Role> loadAndValidateRoles(Set<Integer> requestedIds,
                                            Company company,
                                            RoleScope expectedScope,
                                            Map<Integer, Role> currentRoles) {
        List<Role> roles = new ArrayList<>(roleRepository.findAllById(requestedIds));
        if (roles.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("Uno o más roles seleccionados no existen");
        }

        for (Role role : roles) {
            if (!role.isActivo()) {
                throw new IllegalArgumentException("No se puede asignar un rol inactivo");
            }
            if (role.getScope() != expectedScope) {
                throw new AccessDeniedException("El rol no corresponde al tipo de cuenta");
            }

            boolean existingAssignment = currentRoles.containsKey(role.getId());
            boolean globalCompanyAdmin = role.getPurpose() == RolePurpose.COMPANY_ADMIN
                    && role.getCompany() == null;
            if (globalCompanyAdmin) {
                if (!existingAssignment && !SecurityUtils.isSuperAdmin()) {
                    throw new AccessDeniedException("Solo la plataforma puede asignar el rol de administrador");
                }
                continue;
            }

            if (role.getPurpose() == RolePurpose.PLATFORM_ADMIN
                    || role.getCompany() == null
                    || !Objects.equals(role.getCompany().getId(), company.getId())) {
                throw new AccessDeniedException("El rol no pertenece a la empresa activa");
            }
        }
        return roles;
    }

    private void requireCompanyContext(Company company) {
        if (company == null || company.getId() == null) {
            throw new IllegalArgumentException("No se pudo determinar la empresa de la asignación");
        }
        if (!SecurityUtils.isSuperAdmin()
                && !Objects.equals(company.getId(), SecurityUtils.getCurrentCompanyId())) {
            throw new AccessDeniedException("No puede modificar roles de otra empresa");
        }
    }

    private Set<String> roleNames(java.util.Collection<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
