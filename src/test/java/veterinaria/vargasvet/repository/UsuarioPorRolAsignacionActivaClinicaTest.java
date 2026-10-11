package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UsuarioPorRolAsignacionActivaClinicaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;

    @Test
    void laAsignacionActivaTraeCargadaSuClinicaAunqueElRolSeaGeneral() {
        Company clinica = new Company();
        clinica.setName("Clínica Patitas");
        clinica.setSlug("clinica-" + UUID.randomUUID());
        clinica.setRuc("20" + System.nanoTime());
        clinica.setActivo(true);
        entityManager.persist(clinica);

        Role rolGeneral = new Role();
        rolGeneral.setName("ROLE_ADMIN_" + UUID.randomUUID());
        rolGeneral.setScope(RoleScope.STAFF);
        rolGeneral.setPurpose(RolePurpose.COMPANY_ADMIN);
        rolGeneral.setActivo(true);
        entityManager.persist(rolGeneral);

        Usuario usuario = new Usuario();
        usuario.setEmail("ana-" + UUID.randomUUID() + "@example.test");
        usuario.setUsername("ana-" + UUID.randomUUID());
        usuario.setNombre("Ana");
        usuario.setApellido("Test");
        usuario.setCompany(clinica);
        usuario.setActivo(true);
        entityManager.persist(usuario);

        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rolGeneral);
        asignacion.setCompany(clinica);
        entityManager.persist(asignacion);
        entityManager.flush();
        entityManager.clear();

        UsuarioPorRol encontrada = usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(
                        usuario.getId(), rolGeneral.getId(), clinica.getId()).orElseThrow();
        entityManager.detach(encontrada);

        assertThat(encontrada.getRol().getCompany()).isNull();
        assertThat(Hibernate.isInitialized(encontrada.getCompany())).isTrue();
        assertThat(encontrada.getCompany().getSlug()).isEqualTo(clinica.getSlug());
    }

    @Test
    void elMismoUsuarioYRolSeResuelvenSoloDentroDeLaEmpresaDelToken() {
        Company clinicaA = company("Clínica A");
        Company clinicaB = company("Clínica B");

        Role rolGeneral = new Role();
        rolGeneral.setName("ROLE_ADMIN_" + UUID.randomUUID());
        rolGeneral.setScope(RoleScope.STAFF);
        rolGeneral.setPurpose(RolePurpose.COMPANY_ADMIN);
        rolGeneral.setActivo(true);
        entityManager.persist(rolGeneral);

        Usuario usuario = new Usuario();
        usuario.setEmail("multi-" + UUID.randomUUID() + "@example.test");
        usuario.setUsername("multi-" + UUID.randomUUID());
        usuario.setNombre("Multi");
        usuario.setApellido("Empresa");
        usuario.setCompany(clinicaA);
        usuario.setActivo(true);
        entityManager.persist(usuario);

        entityManager.persist(assignment(usuario, rolGeneral, clinicaA));
        entityManager.persist(assignment(usuario, rolGeneral, clinicaB));
        entityManager.flush();
        entityManager.clear();

        var asignacionA = usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(
                        usuario.getId(), rolGeneral.getId(), clinicaA.getId()).orElseThrow();
        var asignacionB = usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(
                        usuario.getId(), rolGeneral.getId(), clinicaB.getId()).orElseThrow();

        assertThat(asignacionA.getCompany().getId()).isEqualTo(clinicaA.getId());
        assertThat(asignacionB.getCompany().getId()).isEqualTo(clinicaB.getId());
        assertThat(usuarioPorRolRepository.existsByUsuarioIdAndRolIdAndCompanyId(
                usuario.getId(), rolGeneral.getId(), clinicaA.getId())).isTrue();
        assertThat(usuarioPorRolRepository.existsByUsuarioIdAndRolIdAndCompanyId(
                usuario.getId(), rolGeneral.getId(), clinicaB.getId())).isTrue();
        assertThat(usuarioPorRolRepository.existsByUsuarioIdAndRolIdAndCompanyId(
                usuario.getId(), rolGeneral.getId(), null)).isFalse();
        assertThat(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(
                usuario.getId(), rolGeneral.getId(), null)).isEmpty();
    }

    private Company company(String name) {
        Company company = new Company();
        company.setName(name);
        company.setSlug("clinica-" + UUID.randomUUID());
        company.setRuc(String.format("20%09d", Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000_000)));
        company.setActivo(true);
        entityManager.persist(company);
        return company;
    }

    private UsuarioPorRol assignment(Usuario usuario, Role role, Company company) {
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setUsuario(usuario);
        assignment.setRol(role);
        assignment.setCompany(company);
        return assignment;
    }
}
