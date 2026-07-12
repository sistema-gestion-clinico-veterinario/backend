package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.request.CitaReprogramacionRequest;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.mapper.CitaMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.util.BusinessValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
class CitaServiceIntegrationTest {

    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private HistoriaClinicaRepository historiaClinicaRepository;
    @Autowired private ConsultaRepository consultaRepository;
    @Autowired private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Autowired private CompanyExceptionRepository companyExceptionRepository;
    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;

    private CitaServiceImpl citaService;

    @BeforeEach
    void setUp() {
        CitaMapper citaMapper = mock(CitaMapper.class);
        when(citaMapper.toResponse(any(Cita.class))).thenReturn(new CitaResponse());

        citaService = new CitaServiceImpl(
                citaRepository,
                mascotaRepository,
                empleadoRepository,
                serviciosVeterinariosRepository,
                usuarioRepository,
                historiaClinicaRepository,
                consultaRepository,
                companyOperatingHourRepository,
                companyExceptionRepository,
                horarioEmpleadoRepository,
                citaMapper,
                mock(BusinessValidator.class),
                mock(AccesoValidator.class),
                mock(AuditLogService.class),
                mock(EmailService.class),
                mock(SimpMessagingTemplate.class)
        );
        autenticarSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[BB-009] Iniciar atencion crea consulta e historia clinica")
    void iniciarAtencionCreaHistoriaClinicaConsultaYActualizaEstadoDeCita() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().minusMinutes(20));

        Long consultaId = citaService.iniciarAtencion(cita.getId());

        Cita actualizada = citaRepository.findById(cita.getId()).orElseThrow();
        assertThat(actualizada.getEstado()).isEqualTo(EstadoCita.EN_PROCESO);
        assertThat(historiaClinicaRepository.findByMascotaId(cita.getMascota().getId())).isPresent();
        assertThat(consultaRepository.findById(consultaId).orElseThrow().getEstado()).isEqualTo(EstadoConsulta.ABIERTA);
        assertThat(consultaRepository.findById(consultaId).orElseThrow().getCita().getId()).isEqualTo(cita.getId());
    }

    @Test
    @DisplayName("[BB-004] Agendar una cita valida persiste la cita programada")
    void agendarCitaValidaPersisteCitaProgramada() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        citaRepository.delete(plantilla);
        citaRepository.flush();

        citaService.createCita(request);

        Cita creada = citaRepository.findAll().getFirst();
        assertThat(creada.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(creada.getMascota().getId()).isEqualTo(request.getMascotaId());
        assertThat(creada.getFechaHoraInicio()).isEqualTo(request.getFechaHoraInicio());
    }

    @Test
    @DisplayName("[BB-005] Agendar una cita en fecha pasada es rechazado")
    void agendarCitaEnFechaPasadaEsRechazado() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().minusDays(1));
        citaRepository.delete(plantilla);
        citaRepository.flush();

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser anterior");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[BB-006] Reprogramar una cita valida actualiza fecha y estado")
    void reprogramarCitaValidaActualizaFechaYEstado() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        cita.setEsEmergencia(true);
        citaRepository.saveAndFlush(cita);
        LocalDateTime nuevaFecha = LocalDateTime.now().plusDays(4).withSecond(0).withNano(0);

        citaService.reprogramarCita(cita.getId(), reprogramacion(cita, nuevaFecha));

        Cita actualizada = citaRepository.findById(cita.getId()).orElseThrow();
        assertThat(actualizada.getEstado()).isEqualTo(EstadoCita.REPROGRAMADA);
        assertThat(actualizada.getFechaHoraInicio()).isEqualTo(nuevaFecha);
    }

    @Test
    @DisplayName("[BB-007][DEF-BB-007] Caracteriza que una cita cancelada se reprograma indebidamente")
    void citaCanceladaActualmentePuedeReprogramarse() {
        Cita cita = crearCita(EstadoCita.CANCELADA, LocalDateTime.now().plusDays(2));
        cita.setEsEmergencia(true);
        citaRepository.saveAndFlush(cita);

        citaService.reprogramarCita(
                cita.getId(),
                reprogramacion(cita, LocalDateTime.now().plusDays(4).withSecond(0).withNano(0))
        );

        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.REPROGRAMADA);
    }

    @Test
    @DisplayName("[BB-008] Cancelar una cita activa cambia su estado a cancelada")
    void cancelarCitaActivaCambiaEstadoACancelada() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));

        citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void repositorioDetectaCruceDeHorarioDelVeterinario() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        boolean hayCruce = citaRepository.existsOverlappingCita(
                cita.getEmpleado().getId(),
                cita.getFechaHoraInicio().plusMinutes(10),
                cita.getFechaHoraFin().plusMinutes(10)
        );

        assertThat(hayCruce).isTrue();
    }

    private Cita crearCita(EstadoCita estado, LocalDateTime fechaInicio) {
        Company company = new Company();
        company.setName("VargasVet Citas");
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
        mascota.setNombreCompleto("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario empleadoUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(empleadoUser);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(uniqueDigits(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.save(empleado);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta general");
        servicio.setDescripcion("Consulta veterinaria general");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(fechaInicio);
        cita.setFechaHoraFin(fechaInicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private CitaRequest requestDesde(Cita cita, LocalDateTime fechaInicio) {
        CitaRequest request = new CitaRequest();
        request.setMascotaId(cita.getMascota().getId());
        request.setVeterinarioId(cita.getEmpleado().getId());
        request.setServicioId(cita.getServicio().getId());
        request.setMotivoCita("Control general");
        request.setFechaHoraInicio(fechaInicio.withSecond(0).withNano(0));
        request.setEsEmergencia(true);
        return request;
    }

    private CitaReprogramacionRequest reprogramacion(Cita cita, LocalDateTime fechaInicio) {
        CitaReprogramacionRequest request = new CitaReprogramacionRequest();
        request.setVeterinarioId(cita.getEmpleado().getId());
        request.setFechaHoraInicio(fechaInicio);
        request.setMotivoReprogramacion("Cambio solicitado por el cliente");
        return request;
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("password");
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "doctor@vargasvet.test",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
