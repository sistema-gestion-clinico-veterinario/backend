package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HorarioEmpleadoLecturaRepositoryTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Empleado empleado;
    private Empleado otroEmpleado;

    @BeforeEach
    void datos() {
        Company clinica = new Company();
        clinica.setName("Clinica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);
        empleado = empleado(clinica);
        otroEmpleado = empleado(clinica);

        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(empleado, LUNES.plusDays(1), "09:00", "13:00"),
                nuevo(empleado, LUNES, "15:00", "19:00"),
                nuevo(empleado, LUNES, "09:00", "13:00"),
                nuevo(otroEmpleado, LUNES, "09:00", "13:00")));
        jdbcTemplate.update("INSERT INTO horario_empleado (empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo) "
                + "VALUES (?, NULL, 'MARTES', '10:00:00', '12:00:00', TRUE)", empleado.getId());
        jdbcTemplate.update("UPDATE horario_empleado SET activo = FALSE WHERE empleado_id = ? AND fecha = ? AND hora_inicio = '15:00:00'",
                empleado.getId(), LUNES);
        entityManager.clear();
    }

    @Test
    void devuelveTodosLosTurnosOrdenadosConLosSinFechaPrimeroYLuegoPorFechaYHora() {
        List<HorarioDeEmpleado> horarios = horarioEmpleadoRepository.findHorarioDelEmpleado(empleado.getId());

        assertThat(horarios).extracting(h -> h.fecha() + " " + h.horaInicio())
                .containsExactly("null 10:00", LUNES + " 09:00", LUNES + " 15:00", LUNES.plusDays(1) + " 09:00");
    }

    @Test
    void incluyeLosTurnosInactivosEnElHorarioCompleto() {
        List<HorarioDeEmpleado> horarios = horarioEmpleadoRepository.findHorarioDelEmpleado(empleado.getId());

        assertThat(horarios).extracting(HorarioDeEmpleado::activo).contains(false);
    }

    @Test
    void elHorarioActivoOmiteLosTurnosInactivosYConservaElOrden() {
        List<HorarioDeEmpleado> horarios = horarioEmpleadoRepository.findHorarioActivoDelEmpleado(empleado.getId());

        assertThat(horarios).extracting(h -> h.fecha() + " " + h.horaInicio())
                .containsExactly("null 10:00", LUNES + " 09:00", LUNES.plusDays(1) + " 09:00");
        assertThat(horarios).extracting(HorarioDeEmpleado::activo).containsOnly(true);
    }

    @Test
    void devuelveTodosLosCamposDelTurno() {
        HorarioDeEmpleado turno = horarioEmpleadoRepository.findHorarioActivoDelEmpleado(empleado.getId()).get(1);

        assertThat(turno.id()).isNotNull();
        assertThat(turno.fecha()).isEqualTo(LUNES);
        assertThat(turno.diaSemana()).isEqualTo(DiaSemana.LUNES);
        assertThat(turno.horaInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(turno.horaFin()).isEqualTo(LocalTime.of(13, 0));
        assertThat(turno.activo()).isTrue();
    }

    @Test
    void noMezclaLosTurnosDeOtroEmpleado() {
        assertThat(horarioEmpleadoRepository.findHorarioDelEmpleado(otroEmpleado.getId())).hasSize(1);
        assertThat(horarioEmpleadoRepository.findHorarioDelEmpleado(-1L)).isEmpty();
    }

    private Empleado empleado(Company clinica) {
        Usuario usuario = new Usuario();
        usuario.setEmail("vet-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("vet-" + UUID.randomUUID());
        usuario.setNombre("Vet");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(clinica);
        usuario = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(clinica);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(usuario.getDni());
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        return empleadoRepository.save(empleado);
    }

    private NuevoHorario nuevo(Empleado empleado, LocalDate fecha, String inicio, String fin) {
        return new NuevoHorario(empleado.getId(), fecha, DiaSemana.desde(fecha.getDayOfWeek()),
                LocalTime.parse(inicio), LocalTime.parse(fin), LocalDateTime.of(2026, 11, 11, 9, 0), "admin@empresa.test");
    }
}
