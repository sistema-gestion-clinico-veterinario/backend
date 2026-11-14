package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.DetalleCuentaCita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DetalleCuentaCitaRepositoryTest {

    @Autowired private DetalleCuentaCitaRepository detalleRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Mascota mascota;
    private Cita primera;
    private Cita segunda;
    private Cita tercera;
    private DetalleCuentaCita ultimoDeLaPrimera;
    private DetalleCuentaCita primeroDeLaPrimera;
    private DetalleCuentaCita deLaSegunda;
    private DetalleCuentaCita deLaTercera;

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

        mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        primera = cita("CIT-DET-1", 0);
        segunda = cita("CIT-DET-2", 1);
        tercera = cita("CIT-DET-3", 2);

        ultimoDeLaPrimera = detalle(primera, "Insumo", LocalDateTime.of(2026, 11, 11, 10, 0));
        primeroDeLaPrimera = detalle(primera, "Consulta", LocalDateTime.of(2026, 11, 11, 9, 0));
        deLaSegunda = detalle(segunda, "Vacuna", LocalDateTime.of(2026, 11, 11, 9, 30));
        deLaTercera = detalle(tercera, "Procedimiento", LocalDateTime.of(2026, 11, 11, 9, 45));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void traeLosDetallesDeLasCitasPedidasYNoLosDeOtras() {
        List<DetalleCuentaCita> detalles = detalleRepository.findDeLasCitas(List.of(primera.getId(), segunda.getId()));

        assertThat(detalles).extracting(DetalleCuentaCita::getId)
                .containsExactlyInAnyOrder(primeroDeLaPrimera.getId(), ultimoDeLaPrimera.getId(), deLaSegunda.getId())
                .doesNotContain(deLaTercera.getId());
    }

    @Test
    void losDevuelvePorFechaDeCreacionYLuegoPorIdentificador() {
        List<DetalleCuentaCita> detalles = detalleRepository.findDeLasCitas(List.of(primera.getId(), segunda.getId()));

        assertThat(detalles).extracting(DetalleCuentaCita::getDescripcion)
                .containsExactly("Consulta", "Vacuna", "Insumo");
    }

    @Test
    void conLaMismaFechaDeCreacionDesempataPorIdentificador() {
        DetalleCuentaCita igualUno = detalle(primera, "Igual uno", LocalDateTime.of(2026, 11, 11, 8, 0));
        DetalleCuentaCita igualDos = detalle(primera, "Igual dos", LocalDateTime.of(2026, 11, 11, 8, 0));
        entityManager.flush();
        entityManager.clear();

        List<DetalleCuentaCita> detalles = detalleRepository.findDeLasCitas(List.of(primera.getId()));

        assertThat(detalles).extracting(DetalleCuentaCita::getId).startsWith(igualUno.getId(), igualDos.getId());
    }

    @Test
    void unaSolaSentenciaSinImportarCuantasCitasSePidan() {
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        detalleRepository.findDeLasCitas(List.of(primera.getId(), segunda.getId(), tercera.getId()));

        assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void unaCitaSinDetallesNoDevuelveNada() {
        assertThat(detalleRepository.findDeLasCitas(List.of(-1L))).isEmpty();
    }

    private Cita cita(String numero, int desplazamiento) {
        LocalDateTime inicio = LocalDateTime.of(2026, 12, 7, 9, 0).plusHours(desplazamiento);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setNumeroCita(numero);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setTotalServicio(new BigDecimal("150.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private DetalleCuentaCita detalle(Cita cita, String descripcion, LocalDateTime creadoEn) {
        DetalleCuentaCita detalle = new DetalleCuentaCita();
        detalle.setCita(cita);
        detalle.setTipo(TipoDetalleCuenta.SERVICIO);
        detalle.setDescripcion(descripcion);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("50.00"));
        detalle.setSubtotal(new BigDecimal("50.00"));
        detalle.setEsServicioBase(false);
        detalle.setRegistradoPor("prueba");
        detalle = detalleRepository.save(detalle);
        detalleRepository.flush();
        jdbcTemplate.update("UPDATE detalle_cuenta_cita SET created_at = ? WHERE id = ?", creadoEn, detalle.getId());
        return detalle;
    }
}
