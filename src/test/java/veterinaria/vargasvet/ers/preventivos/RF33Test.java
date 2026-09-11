package veterinaria.vargasvet.ers.preventivos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.TipoVacuna;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoConsulta;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.RegistroDesparasitacionRequest;
import veterinaria.vargasvet.dto.request.RegistroVacunacionRequest;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TipoDesparasitanteRepository;
import veterinaria.vargasvet.repository.TipoVacunaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.impl.ConsultaServiceImpl;
import veterinaria.vargasvet.service.impl.ControlPreventivoServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * RF-33: registro de vacunacion y desparasitacion al cerrar una consulta.
 * Cubre RVA-057 a RVA-063: los datos de cartilla (vacuna/producto, fecha,
 * intervalo, proxima fecha) deben solicitarse y persistirse cuando la
 * decision preventiva es afirmativa, y vacunacion/desparasitacion deben
 * quedar en tablas separadas.
 */
@DataJpaTest
class RF33Test {

    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    @Autowired private ControlPreventivoRepository controlPreventivoRepository;
    @Autowired private RegistroVacunaRepository registroVacunaRepository;
    @Autowired private RegistroDesparasitacionRepository registroDesparasitacionRepository;
    @Autowired private TipoVacunaRepository tipoVacunaRepository;
    @Autowired private TipoDesparasitanteRepository tipoDesparasitanteRepository;

    private ConsultaServiceImpl consultaService;

    @BeforeEach
    void setUp() {
        ControlPreventivoServiceImpl controlPreventivoService = new ControlPreventivoServiceImpl(
                tipoVacunaRepository, tipoDesparasitanteRepository, controlPreventivoRepository,
                registroVacunaRepository, registroDesparasitacionRepository, mascotaRepository,
                consultaRepository, mock(AuditLogService.class), mock(AccesoValidator.class));

        consultaService = new ConsultaServiceImpl(
                consultaRepository, historiaClinicaRepository, mascotaRepository, citaRepository,
                new ConsultaMapper(), mock(AuditLogService.class), mock(AccesoValidator.class));
        ReflectionTestUtils.setField(consultaService, "controlPreventivoService", controlPreventivoService);

        autenticarSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF33-01][RVA-057][RVA-058][RVA-059][RVA-062] Registra la vacuna aplicada con fecha, intervalo y proxima fecha al cerrar")
    void cpRf3301_registraVacunacionAlCerrar() {
        Consulta consulta = crearConsultaAbierta();
        TipoVacuna vacuna = crearTipoVacuna(consulta);

        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(true);
        request.setDesparasitacionAplicada(false);
        RegistroVacunacionRequest registro = new RegistroVacunacionRequest();
        registro.setTipoVacunaId(vacuna.getId());
        registro.setFechaAplicacion(LocalDate.now());
        registro.setPeriodicidadMeses(12);
        request.setRegistroVacunacion(registro);

        consultaService.cerrarConsulta(consulta.getId(), request);

        assertThat(registroVacunaRepository.count()).isEqualTo(1L);
        var persistido = registroVacunaRepository.findAll().get(0);
        assertThat(persistido.getConsulta().getId()).isEqualTo(consulta.getId());
        assertThat(persistido.getTipoVacuna().getId()).isEqualTo(vacuna.getId());
        assertThat(persistido.getFechaAplicacion()).isEqualTo(LocalDate.now());
        assertThat(persistido.getPeriodicidadMeses()).isEqualTo(12);
        assertThat(persistido.getFechaProximaDosis())
                .as("RVA-062: la proxima fecha debe calcularse a partir del intervalo cuando no se indica explicitamente")
                .isEqualTo(LocalDate.now().plusMonths(12));
        assertThat(registroDesparasitacionRepository.count())
                .as("RVA-063: la desparasitacion no debe crear un registro cuando no fue aplicada")
                .isZero();
    }

    @Test
    @DisplayName("[CP-RF33-02][RVA-063] Registra vacunacion y desparasitacion por separado cuando ambas se aplican")
    void cpRf3302_registraAmbasAplicacionesPorSeparado() {
        Consulta consulta = crearConsultaAbierta();
        TipoVacuna vacuna = crearTipoVacuna(consulta);

        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(true);
        request.setDesparasitacionAplicada(true);

        RegistroVacunacionRequest registroVacuna = new RegistroVacunacionRequest();
        registroVacuna.setTipoVacunaId(vacuna.getId());
        registroVacuna.setFechaAplicacion(LocalDate.now());
        registroVacuna.setPeriodicidadMeses(12);
        request.setRegistroVacunacion(registroVacuna);

        RegistroDesparasitacionRequest registroDesparasitacion = new RegistroDesparasitacionRequest();
        registroDesparasitacion.setProducto("Praziquantel QA");
        registroDesparasitacion.setFechaAplicacion(LocalDate.now());
        registroDesparasitacion.setPeriodicidadMeses(3);
        request.setRegistroDesparasitacion(registroDesparasitacion);

        consultaService.cerrarConsulta(consulta.getId(), request);

        assertThat(registroVacunaRepository.count()).isEqualTo(1L);
        assertThat(registroDesparasitacionRepository.count()).isEqualTo(1L);
        var desparasitacion = registroDesparasitacionRepository.findAll().get(0);
        assertThat(desparasitacion.getProducto()).isEqualTo("Praziquantel QA");
        assertThat(desparasitacion.getFechaProximaAplicacion()).isEqualTo(LocalDate.now().plusMonths(3));
        assertThat(desparasitacion.getConsulta().getId()).isEqualTo(consulta.getId());
    }

    @Test
    @DisplayName("[CP-RF33-01] Rechaza el cierre con vacunacion aplicada sin los datos de la vacuna")
    void cpRf3301_rechazaVacunacionAplicadaSinDatos() {
        Consulta consulta = crearConsultaAbierta();
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(true);
        request.setDesparasitacionAplicada(false);

        Long consultaId = consulta.getId();
        assertThatThrownBy(() -> consultaService.cerrarConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacuna aplicada");

        assertThat(registroVacunaRepository.count()).isZero();
        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado())
                .isEqualTo(EstadoConsulta.ABIERTA);
    }

    @Test
    @DisplayName("[CP-RF33-02] Rechaza el cierre con desparasitacion aplicada sin los datos del producto")
    void cpRf3302_rechazaDesparasitacionAplicadaSinDatos() {
        Consulta consulta = crearConsultaAbierta();
        CerrarConsultaRequest request = cierreCompleto(consulta);
        request.setVacunacionAplicada(false);
        request.setDesparasitacionAplicada(true);

        Long consultaId = consulta.getId();
        assertThatThrownBy(() -> consultaService.cerrarConsulta(consultaId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desparasitación aplicada");

        assertThat(registroDesparasitacionRepository.count()).isZero();
        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado())
                .isEqualTo(EstadoConsulta.ABIERTA);
    }

    private CerrarConsultaRequest cierreCompleto(Consulta consulta) {
        CerrarConsultaRequest request = new CerrarConsultaRequest();
        request.setVersion(consulta.getVersion());
        request.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        request.setPesoEnConsulta(10.0);
        request.setAnamnesis("Paciente evaluado y clínicamente estable");
        return request;
    }

    private TipoVacuna crearTipoVacuna(Consulta consulta) {
        TipoVacuna vacuna = new TipoVacuna();
        vacuna.setCompany(consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany());
        vacuna.setNombre("Antirrabica QA " + UUID.randomUUID());
        vacuna.setEspecie(consulta.getHistoriaClinica().getMascota().getEspecie());
        vacuna.setPrecio(new BigDecimal("50.00"));
        vacuna.setActivo(true);
        vacuna.setCreatedBy("qa@test");
        vacuna.setUpdatedBy("qa@test");
        return tipoVacunaRepository.saveAndFlush(vacuna);
    }

    private Consulta crearConsultaAbierta() {
        Cita cita = crearCita();
        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(cita.getMascota());
        historia.setNumeroHc("HC-" + UUID.randomUUID().toString().substring(0, 8));
        historia.setActiva(true);
        historia = historiaClinicaRepository.save(historia);

        Consulta consulta = new Consulta();
        consulta.setHistoriaClinica(historia);
        consulta.setCita(cita);
        consulta.setVeterinario(cita.getEmpleado());
        consulta.setFechaConsulta(LocalDateTime.now());
        consulta.setMotivoConsulta("Control general");
        consulta.setTipoConsulta(TipoConsulta.CONTROL_RUTINA);
        consulta.setEstado(EstadoConsulta.ABIERTA);
        consulta.setPesoEnConsulta(10.0);
        consulta.setAnamnesis("Paciente estable");
        return consultaRepository.saveAndFlush(consulta);
    }

    private Cita crearCita() {
        Company company = new Company();
        company.setName("Empresa QA " + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(uniqueDigits(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Paciente QA");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setPeso(10.0);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario veterinarioUser = usuario("veterinario", company);
        Empleado veterinario = new Empleado();
        veterinario.setUser(veterinarioUser);
        veterinario.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        veterinario.setNumeroDocumentoIdentidad(uniqueDigits(8));
        veterinario.setGenero(Genero.MASCULINO);
        veterinario.setEstado(true);
        veterinario = empleadoRepository.save(veterinario);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta QA");
        servicio.setDescripcion("Servicio controlado para RF-33");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(veterinario);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control general");
        cita.setFechaHoraInicio(LocalDateTime.now().minusMinutes(20));
        cita.setFechaHoraFin(LocalDateTime.now().plusMinutes(10));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.EN_PROCESO);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("credencial-ficticia-no-utilizable");
        usuario.setNombre(prefix);
        usuario.setApellido("QA");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new veterinaria.vargasvet.security.UsuarioPrincipal(
                1, "qa@vargasvet.test", "", authorities, null, 1,
                RoleScope.PLATFORM, RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities));
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
