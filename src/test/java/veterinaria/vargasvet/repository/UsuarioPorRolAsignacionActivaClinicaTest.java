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
                .findActiveAssignmentByUsuarioIdAndRoleId(usuario.getId(), rolGeneral.getId()).orElseThrow();
        entityManager.detach(encontrada);

        assertThat(encontrada.getRol().getCompany()).isNull();
        assertThat(Hibernate.isInitialized(encontrada.getCompany())).isTrue();
        assertThat(encontrada.getCompany().getSlug()).isEqualTo(clinica.getSlug());
    }
}
