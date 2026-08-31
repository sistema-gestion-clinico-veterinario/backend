package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.RolVentanaConfiguracion;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.RolVentanaConfiguracionRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyRoleProvisioningService {

    private static final Set<String> CLIENT_WRITE_VIEWS = Set.of("VISTA_MIS_CITAS");

    private final RoleRepository roleRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final RolVentanaConfiguracionRepository rolVentanaConfiguracionRepository;

    @Transactional
    public CompanyRoles ensureRequiredRoles(Company company) {
        Role companyAdmin = ensureRole(company, "ROLE_ADMINISTRADOR", "Administrador de la empresa",
                RoleScope.STAFF, RolePurpose.COMPANY_ADMIN);
        Role clientPortal = ensureRole(company, "ROLE_CLIENTE", "Cliente del portal",
                RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL);
        return new CompanyRoles(companyAdmin, clientPortal);
    }

    @Transactional
    public void migrateLegacyGlobalAssignments() {
        for (UsuarioPorRol assignment : java.util.List.copyOf(usuarioPorRolRepository.findAll())) {
            Role source = assignment.getRol();
            Company company = assignment.getUsuario().getCompany();
            if (source == null || source.getCompany() != null || company == null
                    || source.getScope() == RoleScope.PLATFORM) continue;

            CompanyRoles required = ensureRequiredRoles(company);
            Role target = switch (source.getPurpose()) {
                case COMPANY_ADMIN -> required.companyAdmin();
                case CLIENT_PORTAL -> required.clientPortal();
                case CUSTOM -> ensureLegacyCustomRole(company, source);
                case PLATFORM_ADMIN -> source;
            };
            if (target.getId().equals(source.getId())) continue;

            if (usuarioPorRolRepository.existsByUsuarioIdAndRolId(
                    assignment.getUsuario().getId(), target.getId())) {
                usuarioPorRolRepository.delete(assignment);
            } else {
                assignment.setRol(target);
                usuarioPorRolRepository.save(assignment);
            }
        }
    }

    private Role ensureLegacyCustomRole(Company company, Role source) {
        return roleRepository.findByNameAndCompanyId(source.getName(), company.getId())
                .orElseGet(() -> {
                    Role target = new Role();
                    target.setName(source.getName());
                    target.setDescripcion(source.getDescripcion());
                    target.setCompany(company);
                    target.setScope(RoleScope.STAFF);
                    target.setPurpose(RolePurpose.CUSTOM);
                    target.setSystemManaged(false);
                    target.setProtectedRole(false);
                    target.setActivo(source.isActivo());
                    target = roleRepository.save(target);
                    copyPermissions(source, target);
                    copyMenuConfiguration(source, target);
                    return target;
                });
    }

    private void copyPermissions(Role source, Role target) {
        for (RolVistaPermiso original : rolVistaPermisoRepository.findByRolId(source.getId())) {
            if (!isCompatible(target.getScope(), original.getVista().getAudience())) continue;
            RolVistaPermiso permission = new RolVistaPermiso();
            permission.setRol(target);
            permission.setVista(original.getVista());
            permission.setLeer(original.isLeer());
            permission.setEscribir(original.isEscribir());
            permission.setModificar(original.isModificar());
            permission.setEliminar(original.isEliminar());
            rolVistaPermisoRepository.save(permission);
        }
    }

    private void copyMenuConfiguration(Role source, Role target) {
        for (RolVentanaConfiguracion original
                : rolVentanaConfiguracionRepository.findByRolIdWithVentana(source.getId())) {
            RolVentanaConfiguracion config = new RolVentanaConfiguracion();
            config.setRol(target);
            config.setVentana(original.getVentana());
            config.setPresentacion(original.getPresentacion());
            config.setOrden(original.getOrden());
            rolVentanaConfiguracionRepository.save(config);
        }
    }

    private Role ensureRole(Company company, String name, String description,
                            RoleScope scope, RolePurpose purpose) {
        return roleRepository.findFirstByCompanyIdAndPurpose(company.getId(), purpose)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescripcion(description);
                    role.setCompany(company);
                    role.setScope(scope);
                    role.setPurpose(purpose);
                    role.setSystemManaged(true);
                    role.setProtectedRole(true);
                    role.setActivo(true);
                    Role saved = roleRepository.save(role);
                    assignDefaultPermissions(saved);
                    return saved;
                });
    }

    private void assignDefaultPermissions(Role role) {
        for (Vista vista : vistaRepository.findByActivoTrue()) {
            if (!isCompatible(role.getScope(), vista.getAudience())) continue;

            RolVistaPermiso permission = new RolVistaPermiso();
            permission.setRol(role);
            permission.setVista(vista);
            permission.setLeer(true);
            boolean companyAdmin = role.getPurpose() == RolePurpose.COMPANY_ADMIN;
            boolean clientCanManage = role.getPurpose() == RolePurpose.CLIENT_PORTAL
                    && CLIENT_WRITE_VIEWS.contains(vista.getCodigo());
            permission.setEscribir(companyAdmin || clientCanManage);
            permission.setModificar(companyAdmin || clientCanManage);
            permission.setEliminar(companyAdmin || clientCanManage);
            rolVistaPermisoRepository.save(permission);
        }
    }

    private boolean isCompatible(RoleScope scope, ViewAudience audience) {
        if (audience == ViewAudience.SHARED) return true;
        return (scope == RoleScope.STAFF && audience == ViewAudience.STAFF)
                || (scope == RoleScope.CLIENT && audience == ViewAudience.CLIENT);
    }

    public record CompanyRoles(Role companyAdmin, Role clientPortal) {}
}
