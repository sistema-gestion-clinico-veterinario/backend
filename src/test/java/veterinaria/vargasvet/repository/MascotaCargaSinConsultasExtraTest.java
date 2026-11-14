package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MascotaCargaSinConsultasExtraTest {

    private static final int MASCOTAS = 30;

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Long apoderadoId;

    @BeforeEach
    void datos() {
        Company clinica = new Company();
        clinica.setName("Clinica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);

        Usuario usuario = new Usuario();
        usuario.setEmail("cliente-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("cliente-" + UUID.randomUUID());
        usuario.setNombre("Cliente");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(clinica);
        usuario = usuarioRepository.save(usuario);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(clinica);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(usuario.getDni());
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);
        apoderadoId = apoderado.getId();

        for (int i = 0; i < MASCOTAS; i++) {
            Mascota mascota = new Mascota();
            mascota.setNombreCompleto("Paciente " + i);
            mascota.setEspecie(EspecieMascota.PERRO);
            mascota.setApoderado(apoderado);
            mascota.setUuid(UUID.randomUUID().toString());
            mascotaRepository.save(mascota);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void cargarVariasMascotasHaceUnaSolaConsultaYNoUnaPorCadaMascota() {
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        List<Mascota> mascotas = mascotaRepository.findByApoderadoId(apoderadoId);

        assertThat(mascotas).hasSize(MASCOTAS);
        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }
}
