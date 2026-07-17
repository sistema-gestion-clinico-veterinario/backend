package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.util.BusinessValidator;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreBusinessServiceUnitTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private VistaRepository vistaRepository;

    @Mock
    private RolVistaPermisoRepository rolVistaPermisoRepository;

    @InjectMocks
    private BusinessValidator businessValidator;

    @Test
    void checkCompanyActiva_lanzaExcepcionCuandoEmpresaEstaInactiva() {
        // Arrange
        Company company = new Company();
        company.setId(7);
        company.setActivo(false);
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));

        // Act
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> businessValidator.checkCompanyActiva(7)
        );

        // Assert
        assertEquals("La empresa está inactiva. No se pueden realizar operaciones de escritura.", ex.getMessage());
    }

    @Test
    void deleteRole_lanzaExcepcionParaRolDelSistema() {
        // Arrange
        RoleServiceImpl roleService = roleService();
        Role role = new Role();
        role.setId(1);
        role.setName("ROLE_ADMIN");
        when(roleRepository.findById(1)).thenReturn(Optional.of(role));

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.deleteRole(1)
        );

        // Assert
        assertEquals("No se puede eliminar un rol del sistema", ex.getMessage());
        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    void createRole_normalizaNombreYRechazaDuplicadoEnEmpresa() {
        // Arrange
        RoleServiceImpl roleService = roleService();
        when(roleRepository.existsByNameAndCompanyId("ROLE_VETERINARIO_JEFE", 3)).thenReturn(true);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.createRole(" veterinario jefe ", "Rol clinico", 3)
        );

        // Assert
        assertEquals("Ya existe un rol con ese nombre en esta empresa", ex.getMessage());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void saveVistasByRole_lanzaExcepcionSiRolEstaInactivo() {
        // Arrange
        RoleServiceImpl roleService = roleService();
        Role role = new Role();
        role.setId(4);
        role.setName("ROLE_RECEPCION");
        role.setActivo(false);
        when(roleRepository.findById(4)).thenReturn(Optional.of(role));

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.saveVistasByRole(4, List.of(new RolVistaPermisoDTO()))
        );

        // Assert
        assertEquals("No se pueden asignar permisos a un rol inactivo", ex.getMessage());
        verify(rolVistaPermisoRepository, never()).deleteByRolId(4);
    }

    @Test
    void getVistasByRole_lanzaResourceNotFoundSiRolNoExiste() {
        // Arrange
        RoleServiceImpl roleService = roleService();
        when(roleRepository.findById(77)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex =
                assertThrows(ResourceNotFoundException.class, () -> roleService.getVistasByRole(77));

        // Assert
        assertEquals(ResourceNotFoundException.class, ex.getClass());
    }

    private RoleServiceImpl roleService() {
        return new RoleServiceImpl(
                roleRepository,
                companyRepository,
                vistaRepository,
                rolVistaPermisoRepository
        );
    }
}
