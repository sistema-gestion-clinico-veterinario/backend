package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Asignar el rol de administrador es cosa de la plataforma; quitarlo exige ser administrador y nunca
 * puede dejar a la empresa sin uno activo. Todo dentro de la empresa del usuario autenticado.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioPorRolServiceTest {

    private static final int COMPANY_ID = 3;

    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock RoleRepository roleRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock AdministratorProtection administratorProtection;
    @InjectMocks UsuarioPorRolService service;

    private Company company;
    private Usuario objetivo;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);
        objetivo = new Usuario();
        objetivo.setId(20);
        objetivo.setCompany(company);
        actuarComo(RolePurpose.CUSTOM, COMPANY_ID);
        lenient().when(usuarioRepository.findById(20)).thenReturn(Optional.of(objetivo));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void actuarComo(RolePurpose purpose, Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "actor@empresa.test", "", List.of(), companyId,
                2, RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Role rol(int id, RolePurpose purpose, Company dueña) {
        Role role = new Role();
        role.setId(id);
        role.setName("ROLE_" + id);
        role.setActivo(true);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(purpose);
        role.setCompany(dueña);
        return role;
    }

    private UsuarioPorRol asignacion(Role role, Company empresa) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setId(500);
        asignacion.setUsuario(objetivo);
        asignacion.setRol(role);
        asignacion.setCompany(empresa);
        return asignacion;
    }

    @Test
    void quienNoEsDeLaPlataformaNoPuedeAsignarElRolAdministrador() {
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        when(roleRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.asignarRol(20, 1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Solo la plataforma puede asignar el rol de administrador");

        verify(usuarioPorRolRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ni_unAdministradorDeEmpresaPuedeAsignarElRolAdministrador() {
        actuarComo(RolePurpose.COMPANY_ADMIN, COMPANY_ID);
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        when(roleRepository.findById(1)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.asignarRol(20, 1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void laPlataformaSiPuedeAsignarElRolAdministrador() {
        actuarComo(RolePurpose.PLATFORM_ADMIN, null);
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        when(roleRepository.findById(1)).thenReturn(Optional.of(admin));
        when(usuarioPorRolRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.asignarRol(20, 1);

        verify(usuarioPorRolRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retirarElRolAdministradorPasaPorLaProteccionDeAdministradoresDeEsaEmpresa() {
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        UsuarioPorRol asignacion = asignacion(admin, company);
        when(usuarioPorRolRepository.findById(500)).thenReturn(Optional.of(asignacion));
        doThrow(new IllegalStateException("No se puede quitar el rol de administrador al único administrador activo"))
                .when(administratorProtection).assertCanRemoveAdministratorRole(objetivo, COMPANY_ID);

        assertThatThrownBy(() -> service.revocarRol(500))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");

        verify(usuarioPorRolRepository, never()).delete(asignacion);
    }

    @Test
    void sinObjecionDeLaProteccionElRolAdministradorSeRetira() {
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        UsuarioPorRol asignacion = asignacion(admin, company);
        when(usuarioPorRolRepository.findById(500)).thenReturn(Optional.of(asignacion));

        service.revocarRol(500);

        verify(administratorProtection).assertCanRemoveAdministratorRole(objetivo, COMPANY_ID);
        verify(usuarioPorRolRepository).delete(asignacion);
    }

    @Test
    void retirarUnRolComunNoConsultaLaProteccion() {
        Role vet = rol(2, RolePurpose.CUSTOM, company);
        UsuarioPorRol asignacion = asignacion(vet, company);
        when(usuarioPorRolRepository.findById(500)).thenReturn(Optional.of(asignacion));

        service.revocarRol(500);

        verifyNoInteractions(administratorProtection);
        verify(usuarioPorRolRepository).delete(asignacion);
    }

    @Test
    void noSePuedeRetirarElRolDeUnaPersonaDeOtraEmpresa() {
        Company otra = new Company();
        otra.setId(99);
        objetivo.setCompany(otra);
        Role admin = rol(1, RolePurpose.COMPANY_ADMIN, null);
        UsuarioPorRol asignacion = asignacion(admin, otra);
        when(usuarioPorRolRepository.findById(500)).thenReturn(Optional.of(asignacion));

        assertThatThrownBy(() -> service.revocarRol(500))
                .isInstanceOf(AccessDeniedException.class);

        verify(usuarioPorRolRepository, never()).delete(asignacion);
        verifyNoInteractions(administratorProtection);
    }
}
