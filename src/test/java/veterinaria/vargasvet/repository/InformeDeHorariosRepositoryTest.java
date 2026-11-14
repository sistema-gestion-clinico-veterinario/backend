package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.HorarioEmpleadoLoteRepository.NuevoHorario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InformeDeHorariosRepositoryTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 12, 7);

    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Company clinica;
    private Company otraClinica;
    private Empleado ana;
    private Empleado luis;
    private Empleado inactiva;
    private Empleado ajeno;

    @BeforeEach
    void datos() {
        clinica = clinica("Clinica Uno");
        otraClinica = clinica("Clinica Dos");
        TipoEmpleado veterinario = tipo(clinica, "Veterinario");
        TipoEmpleado auxiliar = tipo(clinica, "Auxiliar");

        ana = empleado(clinica, "Ana", "Torres", true, Set.of(veterinario, auxiliar));
        luis = empleado(clinica, "Luis", "Vega", true, Set.of());
        inactiva = empleado(clinica, "Eva", "Rojas", false, Set.of(auxiliar));
        ajeno = empleado(otraClinica, "Pedro", "Ajeno", true, Set.of());

        horarioEmpleadoRepository.insertarEnLote(List.of(
                turno(ana, LUNES.plusDays(1), "09:00"),
                turno(ana, LUNES, "15:00"),
                turno(ana, LUNES, "09:00"),
                turno(inactiva, LUNES, "10:00"),
                turno(ajeno, LUNES, "10:00")));
        jdbcTemplate.update("INSERT INTO horario_empleado (empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo) "
                + "VALUES (?, NULL, 'MARTES', '08:00:00', '12:00:00', FALSE)", ana.getId());
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void elInformeIncluyeATodosLosEmpleadosDeLaClinicaTambienLosInactivos() {
        List<EmpleadoDelInforme> empleados = empleadoRepository.findEmpleadosDelInformeDeHorarios(clinica.getId());

        assertThat(empleados).extracting(EmpleadoDelInforme::empleadoId)
                .containsExactly(ana.getId(), luis.getId(), inactiva.getId());
    }

    @Test
    void noMezclaEmpleadosDeOtraClinica() {
        assertThat(empleadoRepository.findEmpleadosDelInformeDeHorarios(otraClinica.getId()))
                .extracting(EmpleadoDelInforme::empleadoId).containsExactly(ajeno.getId());
    }

    @Test
    void armaElNombreCompletoConNombreYApellido() {
        assertThat(empleadoRepository.findEmpleadosDelInformeDeHorarios(clinica.getId()).getFirst().nombreCompleto())
                .isEqualTo("Ana Torres");
    }

    @Test
    void elCargoEsPersonalSiElEmpleadoNoTieneTipoYElPrimeroAlfabeticoSiTieneVarios() {
        List<EmpleadoDelInforme> empleados = empleadoRepository.findEmpleadosDelInformeDeHorarios(clinica.getId());

        assertThat(empleados).extracting(EmpleadoDelInforme::cargo).containsExactly("Auxiliar", "Personal", "Auxiliar");
    }

    @Test
    void unEmpleadoConVariosTiposApareceUnaSolaVez() {
        assertThat(empleadoRepository.findEmpleadosDelInformeDeHorarios(clinica.getId()))
                .extracting(EmpleadoDelInforme::empleadoId).containsOnlyOnce(ana.getId());
    }

    @Test
    void losTurnosDeLaClinicaVienenPorEmpleadoLuegoPorFechaConLosSinFechaPrimeroYLuegoPorHora() {
        List<HorarioDeEmpleado> horarios = horarioEmpleadoRepository.findHorariosDeLaEmpresa(clinica.getId());

        assertThat(horarios).extracting(h -> h.empleadoId() + " " + h.fecha() + " " + h.horaInicio())
                .containsExactly(
                        ana.getId() + " null 08:00",
                        ana.getId() + " " + LUNES + " 09:00",
                        ana.getId() + " " + LUNES + " 15:00",
                        ana.getId() + " " + LUNES.plusDays(1) + " 09:00",
                        inactiva.getId() + " " + LUNES + " 10:00");
    }

    @Test
    void elInformeIncluyeLosTurnosInactivosYNoMezclaOtraClinica() {
        List<HorarioDeEmpleado> horarios = horarioEmpleadoRepository.findHorariosDeLaEmpresa(clinica.getId());

        assertThat(horarios).extracting(HorarioDeEmpleado::activo).contains(false);
        assertThat(horarios).extracting(HorarioDeEmpleado::empleadoId).doesNotContain(ajeno.getId());
    }

    @Test
    void cadaTurnoTraeTodosSusCampos() {
        HorarioDeEmpleado turno = horarioEmpleadoRepository.findHorariosDeLaEmpresa(clinica.getId()).get(1);

        assertThat(turno.id()).isNotNull();
        assertThat(turno.empleadoId()).isEqualTo(ana.getId());
        assertThat(turno.fecha()).isEqualTo(LUNES);
        assertThat(turno.diaSemana()).isEqualTo(DiaSemana.LUNES);
        assertThat(turno.horaInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(turno.horaFin()).isEqualTo(LocalTime.of(13, 0));
        assertThat(turno.activo()).isTrue();
    }

    @Test
    void lasConsultasPorEmpleadoSiguenTrayendoElIdentificadorDelEmpleado() {
        assertThat(horarioEmpleadoRepository.findHorarioDelEmpleado(ana.getId()))
                .extracting(HorarioDeEmpleado::empleadoId).containsOnly(ana.getId());
    }

    private Company clinica(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("c-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private TipoEmpleado tipo(Company empresa, String nombre) {
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setNombre(nombre);
        tipo.setCompany(empresa);
        return tipoEmpleadoRepository.save(tipo);
    }

    private Empleado empleado(Company empresa, String nombre, String apellido, boolean activo, Set<TipoEmpleado> tipos) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@clinica.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(empresa);
        usuario = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUser(usuario);
        empleado.setCompany(empresa);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(usuario.getDni());
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(activo);
        empleado.setTiposEmpleado(new HashSet<>(tipos));
        return empleadoRepository.save(empleado);
    }

    private NuevoHorario turno(Empleado empleado, LocalDate fecha, String inicio) {
        return new NuevoHorario(empleado.getId(), fecha, DiaSemana.desde(fecha.getDayOfWeek()),
                LocalTime.parse(inicio), LocalTime.parse(inicio).plusHours(4), LocalDateTime.of(2026, 11, 11, 9, 0), "admin@empresa.test");
    }
}
