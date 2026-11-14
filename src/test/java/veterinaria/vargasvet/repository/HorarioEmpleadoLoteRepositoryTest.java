package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class HorarioEmpleadoLoteRepositoryTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private EntityManager entityManager;

    private Empleado empleado;
    private final LocalDateTime creadoEn = LocalDateTime.of(2026, 11, 11, 9, 30);

    @BeforeEach
    void datos() {
        Company clinica = new Company();
        clinica.setName("Clinica Patitas");
        clinica.setSlug("patitas-" + UUID.randomUUID());
        clinica.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);

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

        empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(clinica);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(usuario.getDni());
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.save(empleado);
        entityManager.flush();
    }

    @Test
    void insertaTodosLosHorariosConTodosSusCampos() {
        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(LUNES, "09:00", "13:00"),
                nuevo(LUNES.plusDays(1), "15:00", "19:00")));
        entityManager.clear();

        List<HorarioEmpleado> guardados = horarioEmpleadoRepository.findByEmpleadoId(empleado.getId());

        assertThat(guardados).hasSize(2);
        HorarioEmpleado lunes = guardados.stream().filter(h -> h.getFecha().equals(LUNES)).findFirst().orElseThrow();
        assertThat(lunes.getDiaSemana()).isEqualTo(DiaSemana.LUNES);
        assertThat(lunes.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(lunes.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
        assertThat(lunes.getActivo()).isTrue();
        assertThat(lunes.getCreatedAt()).isEqualTo(creadoEn);
        assertThat(lunes.getCreatedBy()).isEqualTo("admin@empresa.test");
        assertThat(lunes.getUpdatedAt()).isNull();
        assertThat(lunes.getUpdatedBy()).isNull();
    }

    @Test
    void unaListaVaciaNoHaceNada() {
        horarioEmpleadoRepository.insertarEnLote(List.of());

        assertThat(horarioEmpleadoRepository.findByEmpleadoId(empleado.getId())).isEmpty();
    }

    @Test
    void rechazaDosTurnosConLaMismaFechaYHoraDeInicio() {
        horarioEmpleadoRepository.insertarEnLote(List.of(nuevo(LUNES, "09:00", "13:00")));

        assertThatThrownBy(() -> horarioEmpleadoRepository.insertarEnLote(List.of(nuevo(LUNES, "09:00", "11:00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void losTurnosInsertadosSeVenEnLasConsultasDeTraslape() {
        horarioEmpleadoRepository.insertarEnLote(List.of(nuevo(LUNES, "09:00", "13:00")));

        assertThat(horarioEmpleadoRepository.existsOverlap(empleado.getId(), LUNES, LocalTime.of(12, 0), LocalTime.of(14, 0))).isTrue();
        assertThat(horarioEmpleadoRepository.existsOverlap(empleado.getId(), LUNES, LocalTime.of(13, 0), LocalTime.of(14, 0))).isFalse();
    }

    @Test
    void lasConsultasDeTurnosOcupadosDevuelvenSoloElRangoPedido() {
        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(LUNES, "09:00", "13:00"),
                nuevo(LUNES.plusDays(1), "09:00", "13:00"),
                nuevo(LUNES.plusDays(10), "09:00", "13:00")));
        entityManager.clear();

        List<HorarioEmpleadoRepository.TurnoOcupado> ocupados =
                horarioEmpleadoRepository.findTurnosOcupados(empleado.getId(), LUNES, LUNES.plusDays(5));

        assertThat(ocupados).extracting(HorarioEmpleadoRepository.TurnoOcupado::getFecha)
                .containsExactlyInAnyOrder(LUNES, LUNES.plusDays(1));
    }

    @Test
    void eliminarUnTurnoBorraSoloEseTurno() {
        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(LUNES, "09:00", "13:00"),
                nuevo(LUNES, "15:00", "19:00"),
                nuevo(LUNES.plusDays(1), "09:00", "13:00")));

        horarioEmpleadoRepository.eliminarTurno(empleado.getId(), LUNES, LocalTime.of(9, 0));
        entityManager.clear();

        assertThat(horarioEmpleadoRepository.findByEmpleadoId(empleado.getId()))
                .extracting(h -> h.getFecha() + " " + h.getHoraInicio())
                .containsExactlyInAnyOrder(LUNES + " 15:00", LUNES.plusDays(1) + " 09:00");
    }

    @Test
    void insertaUnaFilaDePlantillaSemanalConFechaNulaYRespetaElEstadoActivo() {
        horarioEmpleadoRepository.insertarEnLote(List.of(
                new NuevoHorario(empleado.getId(), null, DiaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        false, creadoEn, "admin@empresa.test")));
        entityManager.clear();

        HorarioEmpleado plantilla = horarioEmpleadoRepository.findByEmpleadoId(empleado.getId()).getFirst();

        assertThat(plantilla.getFecha()).isNull();
        assertThat(plantilla.getDiaSemana()).isEqualTo(DiaSemana.MARTES);
        assertThat(plantilla.getActivo()).isFalse();
    }

    @Test
    void eliminarTurnosEntreBorraElRangoInclusivoYNoTocaLaPlantillaNiOtrasFechas() {
        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(LUNES.minusDays(1), "09:00", "13:00"),
                nuevo(LUNES, "09:00", "13:00"),
                nuevo(LUNES, "15:00", "19:00"),
                nuevo(LUNES.plusDays(2), "09:00", "13:00"),
                nuevo(LUNES.plusDays(3), "09:00", "13:00"),
                new NuevoHorario(empleado.getId(), null, DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        creadoEn, "admin@empresa.test")));

        horarioEmpleadoRepository.eliminarTurnosEntre(empleado.getId(), LUNES, LUNES.plusDays(2));
        entityManager.clear();

        assertThat(horarioEmpleadoRepository.findByEmpleadoId(empleado.getId()))
                .extracting(h -> h.getFecha() + " " + h.getHoraInicio())
                .containsExactlyInAnyOrder(LUNES.minusDays(1) + " 09:00", LUNES.plusDays(3) + " 09:00", "null 09:00");
    }

    @Test
    void eliminarTurnosEntreNoTocaLosTurnosDeOtroEmpleado() {
        Empleado otro = otroEmpleado();
        horarioEmpleadoRepository.insertarEnLote(List.of(
                nuevo(LUNES, "09:00", "13:00"),
                new NuevoHorario(otro.getId(), LUNES, DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        creadoEn, "admin@empresa.test")));

        horarioEmpleadoRepository.eliminarTurnosEntre(empleado.getId(), LUNES, LUNES);
        entityManager.clear();

        assertThat(horarioEmpleadoRepository.findByEmpleadoId(empleado.getId())).isEmpty();
        assertThat(horarioEmpleadoRepository.findByEmpleadoId(otro.getId())).hasSize(1);
    }

    @Test
    void eliminarLaPlantillaSemanalBorraSoloLasFilasSinFechaDeEseEmpleado() {
        Empleado otro = otroEmpleado();
        horarioEmpleadoRepository.insertarEnLote(List.of(
                new NuevoHorario(empleado.getId(), null, DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0), creadoEn, "admin@empresa.test"),
                new NuevoHorario(empleado.getId(), null, DiaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(13, 0), creadoEn, "admin@empresa.test"),
                nuevo(LUNES, "09:00", "13:00"),
                new NuevoHorario(otro.getId(), null, DiaSemana.LUNES, LocalTime.of(9, 0), LocalTime.of(13, 0), creadoEn, "admin@empresa.test")));

        horarioEmpleadoRepository.eliminarPlantillaSemanal(empleado.getId());
        entityManager.clear();

        assertThat(horarioEmpleadoRepository.findByEmpleadoId(empleado.getId()))
                .extracting(HorarioEmpleado::getFecha).containsExactly(LUNES);
        assertThat(horarioEmpleadoRepository.findByEmpleadoId(otro.getId())).hasSize(1);
    }

    private Empleado otroEmpleado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("otro-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("otro-" + UUID.randomUUID());
        usuario.setNombre("Otro");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(empleado.getCompany());
        usuario = usuarioRepository.save(usuario);

        Empleado otro = new Empleado();
        otro.setUser(usuario);
        otro.setCompany(empleado.getCompany());
        otro.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        otro.setNumeroDocumentoIdentidad(usuario.getDni());
        otro.setGenero(Genero.MASCULINO);
        otro.setEstado(true);
        otro = empleadoRepository.save(otro);
        entityManager.flush();
        return otro;
    }

    private NuevoHorario nuevo(LocalDate fecha, String inicio, String fin) {
        return new NuevoHorario(empleado.getId(), fecha, DiaSemana.desde(fecha.getDayOfWeek()),
                LocalTime.parse(inicio), LocalTime.parse(fin), creadoEn, "admin@empresa.test");
    }
}
