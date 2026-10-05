package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.ProfileResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTenantIsolationTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UsuarioContactoService contactoService;

    @InjectMocks ProfileServiceImpl service;

    private Company companyA;
    private Company companyB;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        companyA = company(1, "Clínica A", "clinica-a");
        companyB = company(2, "Clínica B", "clinica-b");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("persona@example.test");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setActivo(true);
        usuario.setUsuariosPorRol(new ArrayList<>(List.of(
                assignment(companyA, "ROLE_CLIENTE"),
                assignment(companyB, "ROLE_VETERINARIO")
        )));

        UsuarioPrincipal principal = new UsuarioPrincipal(
                10, usuario.getEmail(), null, List.of(), 1,
                11, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void elPerfilNuncaTomaElPrimerEmpleadoNiRolesDeOtraEmpresa() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(10, 1)).thenReturn(Optional.empty());
        when(apoderadoRepository.findByUserIdAndCompanyId(10, 1)).thenReturn(Optional.empty());
        when(companyRepository.findById(1)).thenReturn(Optional.of(companyA));

        ProfileResponse response = service.getMyProfile();

        assertThat(response.getCompanyName()).isEqualTo("Clínica A");
        assertThat(response.getRoles()).containsExactly("ROLE_CLIENTE");
        assertThat(response.isEmpleado()).isFalse();
        assertThat(response.getEmpleadoId()).isNull();
        assertThat(response.getEspecialidades()).isNull();
        assertThat(response.getHorarios()).isNull();
        verify(empleadoRepository, never()).findActiveByUserId(10);
        verify(empleadoRepository, never()).findAllByCompanyId(1);
        verify(empleadoRepository, never()).findAll();
    }

    private Company company(Integer id, String name, String slug) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setSlug(slug);
        company.setActivo(true);
        return company;
    }

    private UsuarioPorRol assignment(Company company, String name) {
        Role role = new Role();
        role.setName(name);
        role.setActivo(true);
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setCompany(company);
        assignment.setRol(role);
        assignment.setUsuario(usuario);
        return assignment;
    }
}
