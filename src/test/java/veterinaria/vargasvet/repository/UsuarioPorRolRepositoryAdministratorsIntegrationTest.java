package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La consulta que cuenta administradores de una empresa: solo personas con un rol activo de ese
 * propósito asignado en ESA empresa, sin duplicados ni mezclar empresas.
 */
@DataJpaTest
class UsuarioPorRolRepositoryAdministratorsIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UsuarioPorRolRepository repository;

    private Company empresa(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug(nombre + "-" + UUID.randomUUID());
        return companyRepository.saveAndFlush(company);
    }

    private Role rol(String nombre, RolePurpose purpose, boolean activo, Company company) {
        Role role = new Role();
        role.setName(nombre + "-" + UUID.randomUUID());
        role.setPurpose(purpose);
        role.setScope(RoleScope.STAFF);
        role.setActivo(activo);
        role.setCompany(company);
        return roleRepository.saveAndFlush(role);
    }

    private Usuario persona() {
        Usuario usuario = new Usuario();
        usuario.setEmail("u-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername("u-" + UUID.randomUUID());
        usuario.setNombre("Ana");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private void asignar(Usuario usuario, Role role, Company company) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(role);
        asignacion.setCompany(company);
        repository.saveAndFlush(asignacion);
    }

    @Test
    void devuelveSoloLosAdministradoresActivosDeLaEmpresaPedida() {
        Company a = empresa("clinica-a");
        Company b = empresa("clinica-b");
        Role adminA = rol("ADMIN", RolePurpose.COMPANY_ADMIN, true, a);
        Role adminB = rol("ADMIN", RolePurpose.COMPANY_ADMIN, true, b);
        Role adminDesactivado = rol("ADMIN-VIEJO", RolePurpose.COMPANY_ADMIN, false, a);
        Role vet = rol("VET", RolePurpose.CUSTOM, true, a);

        Usuario enA = persona();
        Usuario enB = persona();
        Usuario conRolDesactivado = persona();
        Usuario veterinario = persona();
        asignar(enA, adminA, a);
        asignar(enB, adminB, b);
        asignar(conRolDesactivado, adminDesactivado, a);
        asignar(veterinario, vet, a);

        List<Usuario> resultado = repository.findUsersWithActiveRolePurpose(a.getId(), RolePurpose.COMPANY_ADMIN);

        assertThat(resultado).extracting(Usuario::getId).containsExactly(enA.getId());
    }

    @Test
    void unaPersonaConDosRolesAdministradorApareceUnaSolaVez() {
        Company a = empresa("clinica-a");
        Role adminUno = rol("ADMIN-1", RolePurpose.COMPANY_ADMIN, true, a);
        Role adminDos = rol("ADMIN-2", RolePurpose.COMPANY_ADMIN, true, a);
        Usuario persona = persona();
        asignar(persona, adminUno, a);
        asignar(persona, adminDos, a);

        List<Usuario> resultado = repository.findUsersWithActiveRolePurpose(a.getId(), RolePurpose.COMPANY_ADMIN);

        assertThat(resultado).hasSize(1);
    }
}
