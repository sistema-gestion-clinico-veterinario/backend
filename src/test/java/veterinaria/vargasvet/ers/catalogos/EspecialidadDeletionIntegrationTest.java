package veterinaria.vargasvet.ers.catalogos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.impl.EspecialidadServiceImpl;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EspecialidadDeletionIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private EmpleadoRepository empleadoRepository;

    @BeforeEach
    void authenticateSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new UsuarioPrincipal(1, "qa@system.local", "", authorities, null,
                1, RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cpRf3701_laRestriccionReferencialBloqueaEliminarEspecialidadEnUso() {
        Company company = new Company();
        company.setName("Empresa QA");
        company.setRuc("20999999991");
        company.setActivo(true);
        company = companyRepository.saveAndFlush(company);

        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Cirugía");
        especialidad.setDescripcion("Especialidad QA");
        especialidad.setCompany(company);
        especialidad.setCreatedAt(AppClock.now());
        especialidad = especialidadRepository.saveAndFlush(especialidad);

        Empleado empleado = new Empleado();
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("99999991");
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado.getEspecialidades().add(especialidad);
        empleadoRepository.saveAndFlush(empleado);

        EspecialidadServiceImpl service = new EspecialidadServiceImpl(especialidadRepository, companyRepository);
        Long especialidadId = especialidad.getId();

        assertThatThrownBy(() -> {
            service.delete(especialidadId);
            especialidadRepository.flush();
        }).isInstanceOf(DataAccessException.class);
    }
}
