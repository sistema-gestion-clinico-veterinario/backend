package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVentanaConfiguracionRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RoleServiceImpl service = new RoleServiceImpl(
            roleRepository,
            mock(CompanyRepository.class),
            mock(VistaRepository.class),
            mock(RolVistaPermisoRepository.class),
            mock(VentanaRepository.class),
            mock(RolVentanaConfiguracionRepository.class)
    );

    @Test
    void getAssignableRolesReturnsOnlyActiveRolesFromRequestedScope() {
        Company company = company(7);
        Role activeClient = role(1, company, RoleScope.CLIENT, true);
        Role inactiveClient = role(2, company, RoleScope.CLIENT, false);
        Role activeStaff = role(3, company, RoleScope.STAFF, true);
        when(roleRepository.findByCompanyId(7)).thenReturn(List.of(activeClient, inactiveClient, activeStaff));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var result = service.getAssignableRoles(7, RoleScope.CLIENT);

            assertEquals(List.of(1), result.stream().map(dto -> dto.getId()).toList());
        }
    }

    @Test
    void getAssignableRolesRejectsPlatformScope() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.getAssignableRoles(7, RoleScope.PLATFORM));
        }
    }

    @Test
    void getAssignableRolesRejectsAnotherCompany() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            assertThrows(AccessDeniedException.class,
                    () -> service.getAssignableRoles(8, RoleScope.CLIENT));
        }
    }

    private Company company(int id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private Role role(int id, Company company, RoleScope scope, boolean active) {
        Role role = new Role();
        role.setId(id);
        role.setName("ROLE_" + id);
        role.setCompany(company);
        role.setScope(scope);
        role.setActivo(active);
        return role;
    }
}
