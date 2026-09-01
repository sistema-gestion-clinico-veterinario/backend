package veterinaria.vargasvet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.domain.enums.DataScope;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoValidatorTest {

    @Mock
    private RolVistaPermisoRepository rolVistaPermisoRepository;

    @Mock
    private UsuarioPorRolRepository usuarioPorRolRepository;

    private AccesoValidator validator;
    private Role role;

    @BeforeEach
    void setUp() {
        validator = new AccesoValidator(
                new RolePermissionEvaluator(rolVistaPermisoRepository, usuarioPorRolRepository));
        role = new Role();
        role.setId(20);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(RolePurpose.CUSTOM);

        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);
        when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleId(7, 20))
                .thenReturn(Optional.of(assignment));

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CUALQUIER_NOMBRE"));
        var principal = new UsuarioPrincipal(7, "user@example.com", "", authorities, 3,
                20, RoleScope.STAFF, RolePurpose.CUSTOM, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token", authorities));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autorizaPorPermisoDelRolSinDependerDeSuNombre() {
        RolVistaPermiso permiso = permiso(ViewAudience.STAFF);
        permiso.setLeer(true);
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(20, "VISTA_MASCOTAS"))
                .thenReturn(Optional.of(permiso));

        assertTrue(validator.can("VISTA_MASCOTAS", "LEER"));
        assertFalse(validator.can("VISTA_MASCOTAS", "ELIMINAR"));
    }

    @Test
    void rechazaVistaDeClienteParaRolStaffAunqueTengaFlag() {
        RolVistaPermiso permiso = permiso(ViewAudience.CLIENT);
        permiso.setLeer(true);
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(20, "VISTA_MIS_MASCOTAS"))
                .thenReturn(Optional.of(permiso));

        assertFalse(validator.can("VISTA_MIS_MASCOTAS", "LEER"));
    }

    @Test
    void perfilPlataformaProtegidoMantieneAccesoGlobalPorProposito() {
        role.setScope(RoleScope.PLATFORM);
        role.setPurpose(RolePurpose.PLATFORM_ADMIN);

        assertTrue(validator.can("VISTA_AUDITORIA_ADMIN", "ELIMINAR"));
        assertTrue(validator.canAccessCompanyData("VISTA_CITAS_AGENDA"));
    }

    @Test
    void alcanceDeAgendaEsIndependienteDelPermisoEditar() {
        RolVistaPermiso permiso = permiso(ViewAudience.STAFF);
        permiso.getVista().setCodigo("VISTA_CITAS_AGENDA");
        permiso.setLeer(true);
        permiso.setModificar(false);
        permiso.setDataScope(DataScope.COMPANY);
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(20, "VISTA_CITAS_AGENDA"))
                .thenReturn(Optional.of(permiso));

        assertFalse(validator.can("VISTA_CITAS_AGENDA", "MODIFICAR"));
        assertTrue(validator.canAccessCompanyData("VISTA_CITAS_AGENDA"));
    }

    private RolVistaPermiso permiso(ViewAudience audience) {
        Vista vista = new Vista();
        vista.setCodigo("VISTA_MASCOTAS");
        vista.setAudience(audience);
        vista.setActivo(true);
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setRol(role);
        permiso.setVista(vista);
        return permiso;
    }
}
