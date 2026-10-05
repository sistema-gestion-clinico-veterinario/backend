package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
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
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    @Autowired private veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository relacionRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private CitaServiceImpl citaService;
    private CitaMapper citaMapper;
    private veterinaria.vargasvet.service.PetOwnershipService petOwnershipService;
    private EmailService emailService;
    private veterinaria.vargasvet.service.impl.UsuarioContactoService contactoAvisos;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        contactoAvisos = mock(veterinaria.vargasvet.service.impl.UsuarioContactoService.class);
        citaMapper = mock(CitaMapper.class);
        when(citaMapper.toResponse(any(Cita.class))).thenReturn(new CitaResponse());
        AccesoValidator accesoValidator = mock(AccesoValidator.class);
        when(accesoValidator.canAccessCompanyData("VISTA_CITAS_AGENDA")).thenReturn(true);

        petOwnershipService = new veterinaria.vargasvet.service.PetOwnershipService(
                mascotaRepository, relacionRepository, citaRepository, mock(AuditLogService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(petOwnershipService, "entityManager", entityManager);
        citaService = new CitaServiceImpl(
                citaRepository,
                mascotaRepository,
                empleadoRepository,
                apoderadoRepository,
                serviciosVeterinariosRepository,
                usuarioRepository,
                historiaClinicaRepository,
                consultaRepository,
                companyOperatingHourRepository,
                companyExceptionRepository,
                horarioEmpleadoRepository,
                citaMapper,
                mock(BusinessValidator.class),
                accesoValidator,
                mock(AuditLogService.class),
                emailService,
                mock(SimpMessagingTemplate.class),
                mock(veterinaria.vargasvet.service.impl.UsuarioContactoService.class),
                petOwnershipService,
                new veterinaria.vargasvet.service.OwnerContactPolicy(contactoAvisos)
        );
        autenticarSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF28-02] Iniciar atención crea una sola historia clínica y vincula la consulta")
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
    @DisplayName("[CP-RF28-01] Iniciar atención reutiliza la historia clínica existente")
    void iniciarAtencionReutilizaHistoriaClinicaExistente() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().minusMinutes(20));
        HistoriaClinica existente = new HistoriaClinica();
        existente.setMascota(cita.getMascota());
        existente.setNumeroHc("HC-EXISTENTE-" + UUID.randomUUID().toString().substring(0, 5));
        existente.setActiva(true);
        existente = historiaClinicaRepository.saveAndFlush(existente);
        long historiasAntes = historiaClinicaRepository.count();

        Long consultaId = citaService.iniciarAtencion(cita.getId());

        Consulta consulta = consultaRepository.findById(consultaId).orElseThrow();
        assertThat(consulta.getHistoriaClinica().getId()).isEqualTo(existente.getId());
        assertThat(historiaClinicaRepository.count()).isEqualTo(historiasAntes);
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.EN_PROCESO);
    }

    @Test
    @DisplayName("[CP-RF24-01] Agendar una cita válida persiste una sola cita programada")
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
    @DisplayName("[CP-RF26-01] Reprogramar una cita válida actualiza fecha y estado")
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
    @DisplayName("[CP-RF27-01] Cancelar una cita activa conserva el antecedente cancelado")
    void cancelarCitaActivaCambiaEstadoACancelada() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));

        citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    @DisplayName("[CP-RF27-02] Cancelar la cita de un cliente con correo sin verificar no envía correo y pide avisar por teléfono")
    void cancelarCitaDeClientePendienteNoEnviaCorreoYPideAvisarPorTelefono() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        marcarCorreoSinVerificar(cita);
        when(contactoAvisos.telefono(any(), any())).thenReturn("987654321");

        CitaResponse respuesta = citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(respuesta.getRequiereAvisoManual()).isTrue();
        assertThat(respuesta.getTelefonoAviso()).isEqualTo("987654321");
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("[CP-RF27-02] Cancelar la cita de un cliente con correo verificado le envía el aviso por correo")
    void cancelarCitaDeClienteConCorreoVerificadoEnviaElCorreo() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        String correo = cita.getMascota().getApoderado().getUser().getEmail();

        CitaResponse respuesta = citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(respuesta.getRequiereAvisoManual()).isNull();
        org.mockito.Mockito.verify(emailService).createMail(
                org.mockito.ArgumentMatchers.eq(correo),
                org.mockito.ArgumentMatchers.contains("Cita Cancelada"),
                org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    @DisplayName("[CP-RF26-02] Reprogramar la cita de un cliente con correo sin verificar no envía correo y pide avisar por teléfono")
    void reprogramarCitaDeClientePendienteNoEnviaCorreoYPideAvisarPorTelefono() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        cita.setEsEmergencia(true);
        citaRepository.saveAndFlush(cita);
        marcarCorreoSinVerificar(cita);
        when(contactoAvisos.telefono(any(), any())).thenReturn("912345678");

        CitaResponse respuesta = citaService.reprogramarCita(cita.getId(),
                reprogramacion(cita, LocalDateTime.now().plusDays(4).withSecond(0).withNano(0)));

        assertThat(respuesta.getRequiereAvisoManual()).isTrue();
        assertThat(respuesta.getTelefonoAviso()).isEqualTo("912345678");
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("[CP-RF26-03] Reasignar el veterinario de la cita de un cliente con correo sin verificar pide avisar por teléfono")
    void reasignarVeterinarioDeClientePendienteNoEnviaCorreoYPideAvisarPorTelefono() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        cita.setEsEmergencia(true);
        citaRepository.saveAndFlush(cita);
        marcarCorreoSinVerificar(cita);
        Company company = cita.getEmpleado().getUser().getCompany();
        Empleado otroVeterinario = new Empleado();
        otroVeterinario.setUser(usuario("vet2", company));
        otroVeterinario.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        otroVeterinario.setNumeroDocumentoIdentidad(uniqueDigits(8));
        otroVeterinario.setGenero(Genero.FEMENINO);
        otroVeterinario.setEstado(true);
        otroVeterinario = empleadoRepository.saveAndFlush(otroVeterinario);
        veterinaria.vargasvet.dto.request.CitaReasignacionVeterinarioRequest request =
                new veterinaria.vargasvet.dto.request.CitaReasignacionVeterinarioRequest();
        request.setVeterinarioId(otroVeterinario.getId());

        CitaResponse respuesta = citaService.reasignarVeterinario(cita.getId(), request);

        assertThat(respuesta.getRequiereAvisoManual()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    private Usuario vincularCopropietario(Cita cita) {
        Company company = cita.getMascota().getApoderado().getCompany();
        Usuario usuarioCopropietario = usuario("copropietario", company);
        Apoderado copropietario = new Apoderado();
        copropietario.setUser(usuarioCopropietario);
        copropietario.setCompany(company);
        copropietario.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        copropietario.setNumeroDocumento(uniqueDigits(8));
        copropietario.setGenero(Genero.MASCULINO);
        copropietario = apoderadoRepository.saveAndFlush(copropietario);
        veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion relacion = new veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion();
        relacion.setMascota(cita.getMascota());
        relacion.setApoderado(copropietario);
        relacion.setCompany(company);
        relacion.setTipoRelacion(veterinaria.vargasvet.domain.enums.TipoRelacionMascota.COPROPIETARIO);
        relacion.setPuedeRecibirInformacion(true);
        relacion.setPuedeAutorizarAtencion(true);
        relacion.setPuedeRealizarPagos(true);
        relacion.setFechaInicio(java.time.LocalDate.now().minusDays(2));
        relacion.setActivo(true);
        relacion.setCreatedBy("test");
        relacion.setUpdatedBy("test");
        relacionRepository.saveAndFlush(relacion);
        return usuarioCopropietario;
    }

    @Test
    @DisplayName("[RF-PDP-01] Cancelar una cita avisa por correo al propietario y a cada persona vinculada con permiso de recibir información")
    void cancelarCitaAvisaAlPrincipalYAlCopropietario() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        String correoPrincipal = cita.getMascota().getApoderado().getUser().getEmail();
        String correoCopropietario = vincularCopropietario(cita).getEmail();

        CitaResponse respuesta = citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(respuesta.getRequiereAvisoManual()).isNull();
        org.mockito.Mockito.verify(emailService).createMail(org.mockito.ArgumentMatchers.eq(correoPrincipal),
                org.mockito.ArgumentMatchers.contains("Cita Cancelada"), org.mockito.ArgumentMatchers.anyMap());
        org.mockito.Mockito.verify(emailService).createMail(org.mockito.ArgumentMatchers.eq(correoCopropietario),
                org.mockito.ArgumentMatchers.contains("Cita Cancelada"), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    @DisplayName("[RF-PDP-01] Si el propietario no tiene correo verificado, el copropietario recibe el aviso y la clínica igual llama al propietario")
    void conElPrincipalSinCorreoVerificadoElCopropietarioRecibeElAvisoYSePideLlamarAlPrincipal() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        String correoCopropietario = vincularCopropietario(cita).getEmail();
        marcarCorreoSinVerificar(cita);
        when(contactoAvisos.telefono(any(), any())).thenReturn("987654321");

        CitaResponse respuesta = citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(respuesta.getRequiereAvisoManual()).isTrue();
        org.mockito.Mockito.verify(emailService).createMail(org.mockito.ArgumentMatchers.eq(correoCopropietario),
                org.mockito.ArgumentMatchers.contains("Cita Cancelada"), org.mockito.ArgumentMatchers.anyMap());
        org.mockito.Mockito.verify(emailService, org.mockito.Mockito.times(1)).createMail(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    @DisplayName("[RF-PDP-01] Un propietario dado de baja no recibe el aviso de la cita; quien sigue vinculado sí")
    void unPrincipalDadoDeBajaNoRecibeElAvisoDeLaCita() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        String correoPrincipal = cita.getMascota().getApoderado().getUser().getEmail();
        String correoCopropietario = vincularCopropietario(cita).getEmail();
        Apoderado principal = cita.getMascota().getApoderado();
        principal.setEstado(false);
        principal.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        apoderadoRepository.saveAndFlush(principal);

        CitaResponse respuesta = citaService.cancelarCita(cita.getId(), "Solicitud del cliente");

        assertThat(respuesta.getRequiereAvisoManual()).isNull();
        org.mockito.Mockito.verify(emailService).createMail(org.mockito.ArgumentMatchers.eq(correoCopropietario),
                org.mockito.ArgumentMatchers.contains("Cita Cancelada"), org.mockito.ArgumentMatchers.anyMap());
        org.mockito.Mockito.verify(emailService, org.mockito.Mockito.never()).createMail(org.mockito.ArgumentMatchers.eq(correoPrincipal),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
    }

    private void marcarCorreoSinVerificar(Cita cita) {
        Usuario propietario = cita.getMascota().getApoderado().getUser();
        propietario.setActivo(false);
        propietario.setEmailVerified(false);
        usuarioRepository.saveAndFlush(propietario);
    }

    @Test
    @DisplayName("[CP-RF24-02][PARCIAL] El repositorio detecta un cruce del veterinario")
    void repositorioDetectaCruceDeHorarioDelVeterinario() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        boolean hayCruce = citaRepository.existsOverlappingCita(
                cita.getEmpleado().getId(),
                cita.getFechaHoraInicio().plusMinutes(10),
                cita.getFechaHoraFin().plusMinutes(10)
        );

        assertThat(hayCruce).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("[CP-RF24-02] Dos altas concurrentes para la misma franja conservan una sola cita")
    void altasConcurrentesParaLaMismaFranjaConservanUnaSolaCita() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CitaRequest request = transaction.execute(status -> {
            Cita plantilla = crearCita(EstadoCita.PROGRAMADA,
                    LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0));
            CitaRequest result = requestDesde(plantilla, plantilla.getFechaHoraInicio().plusDays(1));
            citaRepository.delete(plantilla);
            citaRepository.flush();
            return result;
        });

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> primera = executor.submit(() -> ejecutarAltaConcurrente(transaction, request, inicio));
            Future<Throwable> segunda = executor.submit(() -> ejecutarAltaConcurrente(transaction, request, inicio));
            inicio.countDown();

            List<Throwable> resultados = Arrays.asList(primera.get(), segunda.get());
            assertThat(resultados).filteredOn(resultado -> resultado == null).hasSize(1);
            assertThat(resultados).filteredOn(resultado -> resultado instanceof IllegalArgumentException).hasSize(1);
            Long citasPersistidas = transaction.execute(status -> citaRepository.count());
            assertThat(citasPersistidas).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("[CP-RF24-04] Agendar una cita y dar de baja a su dueño a la vez nunca deja una cita vigente de un cliente dado de baja")
    void agendarYDarDeBajaAlDuenoALaVezNoDejaUnaCitaDeUnClienteDeBaja() throws Exception {
        veterinaria.vargasvet.service.SessionSecurityService sesiones =
                mock(veterinaria.vargasvet.service.SessionSecurityService.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            Thread.sleep(400);
            return null;
        }).when(sesiones).invalidateSessionsForCompany(any(), any());
        when(citaMapper.toResponse(any(Cita.class))).thenAnswer(invocation -> {
            Thread.sleep(400);
            return new CitaResponse();
        });
        veterinaria.vargasvet.service.impl.ApoderadoServiceImpl apoderadoService =
                new veterinaria.vargasvet.service.impl.ApoderadoServiceImpl(
                        usuarioRepository, apoderadoRepository, mascotaRepository,
                        mock(veterinaria.vargasvet.repository.RefreshTokenRepository.class),
                        mock(veterinaria.vargasvet.repository.UsuarioPorRolRepository.class),
                        mock(veterinaria.vargasvet.repository.RoleRepository.class), companyRepository,
                        mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                        mock(veterinaria.vargasvet.mapper.UserMapper.class), mock(BusinessValidator.class), emailService,
                        mock(AuditLogService.class), mock(veterinaria.vargasvet.service.CompanyRoleProvisioningService.class),
                        sesiones, mock(veterinaria.vargasvet.service.CompanyMembershipService.class), citaRepository,
                        mock(veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository.class),
                        mock(veterinaria.vargasvet.service.impl.UsuarioContactoService.class), petOwnershipService,
                        mock(veterinaria.vargasvet.service.AccountClosureGuard.class),
                        mock(veterinaria.vargasvet.service.AdministratorProtection.class),
                        mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(apoderadoService, "entityManager", entityManager);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        for (boolean agendaPrimero : new boolean[]{true, false}) {
            Object[] datos = transaction.execute(status -> {
                Cita plantilla = crearCita(EstadoCita.PROGRAMADA,
                        LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0));
                CitaRequest request = requestDesde(plantilla, plantilla.getFechaHoraInicio().plusDays(1));
                Long apoderadoId = plantilla.getMascota().getApoderado().getId();
                citaRepository.delete(plantilla);
                citaRepository.flush();
                return new Object[]{request, apoderadoId};
            });
            CitaRequest request = (CitaRequest) datos[0];
            Long apoderadoId = (Long) datos[1];
            Runnable agendar = () -> citaService.createCita(request);
            Runnable baja = () -> apoderadoService.cambiarEstado(apoderadoId, false,
                    veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Prueba");

            CountDownLatch inicio = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Throwable> primera = executor.submit(
                        () -> ejecutarConEspera(transaction, agendaPrimero ? agendar : baja, inicio, 0));
                Future<Throwable> segunda = executor.submit(
                        () -> ejecutarConEspera(transaction, agendaPrimero ? baja : agendar, inicio, 100));
                inicio.countDown();
                List<Throwable> resultados = Arrays.asList(primera.get(), segunda.get());
                assertThat(resultados).filteredOn(r -> r != null && !(r instanceof IllegalArgumentException)).isEmpty();
            } finally {
                executor.shutdownNow();
            }

            Boolean violado = transaction.execute(status -> {
                Apoderado dueno = apoderadoRepository.findById(apoderadoId).orElseThrow();
                return !Boolean.TRUE.equals(dueno.getEstado())
                        && citaRepository.existsCitaVigenteByApoderadoId(apoderadoId, LocalDateTime.now());
            });
            assertThat(violado)
                    .as("cliente dado de baja con una cita vigente (agenda primero: %s)", agendaPrimero)
                    .isFalse();
        }
    }

    private Throwable ejecutarConEspera(TransactionTemplate transaction, Runnable accion, CountDownLatch inicio, long esperaMs) {
        try {
            autenticarSuperAdmin();
            inicio.await();
            Thread.sleep(esperaMs);
            transaction.executeWithoutResult(status -> accion.run());
            return null;
        } catch (Throwable error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("[CP-RF24-03] Rechaza crear una cita para una mascota inactiva sin persistirla")
    void crearCitaRechazaMascotaInactiva() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        Mascota mascota = plantilla.getMascota();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        mascota.setActivo(false);
        mascotaRepository.saveAndFlush(mascota);

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mascota");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF24-03] Rechaza crear una cita cuando el propietario está suspendido o dado de baja")
    void crearCitaRechazaPropietarioInactivo() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        Apoderado propietario = plantilla.getMascota().getApoderado();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        propietario.setEstado(false);
        propietario.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);
        apoderadoRepository.saveAndFlush(propietario);

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propietario");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF24-04] Un cliente pendiente de activar sí puede tener cita, y se avisa por teléfono porque su correo no está verificado")
    void crearCitaParaClientePendienteDeActivarAvisaPorTelefono() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        Usuario propietario = plantilla.getMascota().getApoderado().getUser();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        propietario.setActivo(false);
        propietario.setEmailVerified(false);
        usuarioRepository.saveAndFlush(propietario);

        CitaResponse creada = citaService.createCita(request);

        assertThat(citaRepository.findAll()).hasSize(1);
        assertThat(creada.getRequiereAvisoManual()).isTrue();
    }

    @Test
    @DisplayName("[CP-RF24-04] Con un correo verificado no se pide avisar por teléfono")
    void crearCitaConCorreoVerificadoNoPideAvisoManual() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        citaRepository.delete(plantilla);
        citaRepository.flush();

        CitaResponse creada = citaService.createCita(request);

        assertThat(creada.getRequiereAvisoManual()).isNull();
    }

    @Test
    @DisplayName("[CP-RF24-03] Rechaza crear una cita cuando el empleado está inactivo")
    void crearCitaRechazaEmpleadoInactivo() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        Empleado empleado = plantilla.getEmpleado();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        empleado.setEstado(false);
        empleadoRepository.saveAndFlush(empleado);

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empleado inactivo");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF24-01][RVA-012] Rechaza crear una cita con un servicio inactivo sin persistirla")
    void crearCitaRechazaServicioInactivo() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        ServiciosVeterinarios servicio = plantilla.getServicio();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        servicio.setActivo(false);
        servicio.setDisponible(true);
        serviciosVeterinariosRepository.saveAndFlush(servicio);

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("servicio inactivo");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF24-01][RVA-012] Rechaza crear una cita con un servicio no disponible sin persistirla")
    void crearCitaRechazaServicioNoDisponible() {
        Cita plantilla = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        CitaRequest request = requestDesde(plantilla, LocalDateTime.now().plusDays(3));
        ServiciosVeterinarios servicio = plantilla.getServicio();
        citaRepository.delete(plantilla);
        citaRepository.flush();
        servicio.setActivo(true);
        servicio.setDisponible(false);
        serviciosVeterinariosRepository.saveAndFlush(servicio);

        assertThatThrownBy(() -> citaService.createCita(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("servicio no disponible");
        assertThat(citaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("[CP-RF26-02] Rechaza reprogramar cuando falta una hora o menos")
    void reprogramarCitaDentroDeUnaHoraEsRechazado() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusMinutes(30));
        cita.setEsEmergencia(true);
        citaRepository.saveAndFlush(cita);
        LocalDateTime fechaOriginal = cita.getFechaHoraInicio();

        assertThatThrownBy(() -> citaService.reprogramarCita(
                cita.getId(),
                reprogramacion(cita, LocalDateTime.now().plusDays(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menos de 1 horas");

        Cita sinCambios = citaRepository.findById(cita.getId()).orElseThrow();
        assertThat(sinCambios.getFechaHoraInicio()).isEqualTo(fechaOriginal);
        assertThat(sinCambios.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
    }

    private Mascota otraMascota(String nombre, Apoderado dueno) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.GATO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.saveAndFlush(mascota);
    }

    private CitaRequest edicionConOtraMascota(Cita cita, Mascota nueva) {
        CitaRequest request = requestDesde(cita, LocalDateTime.now().plusDays(3));
        request.setMascotaId(nueva.getId());
        return request;
    }

    private Cita citaConVeterinarioDeLaClinica() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().plusDays(2));
        Empleado veterinario = cita.getEmpleado();
        veterinario.setCompany(cita.getMascota().getApoderado().getCompany());
        empleadoRepository.saveAndFlush(veterinario);
        return cita;
    }

    @Test
    @DisplayName("[CP-RF19-01] Editar una cita para cambiarle la mascota exige una mascota activa de la misma clínica con propietario activo")
    void editarCitaConOtraMascotaExigeMascotaOperableDeLaMismaClinica() {
        Cita cita = citaConVeterinarioDeLaClinica();
        Apoderado dueno = cita.getMascota().getApoderado();
        Mascota gato = otraMascota("Michi", dueno);

        citaService.actualizarCita(cita.getId(), edicionConOtraMascota(cita, gato));
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getMascota().getId()).isEqualTo(gato.getId());

        Cita otra = citaConVeterinarioDeLaClinica();
        Apoderado duenoDeOtraClinica = otra.getMascota().getApoderado();
        Mascota ajena = otraMascota("Ajena", duenoDeOtraClinica);
        assertThatThrownBy(() -> citaService.actualizarCita(cita.getId(), edicionConOtraMascota(cita, ajena)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra clínica");

        Mascota inactiva = otraMascota("Dormida", dueno);
        inactiva.setActivo(false);
        mascotaRepository.saveAndFlush(inactiva);
        assertThatThrownBy(() -> citaService.actualizarCita(cita.getId(), edicionConOtraMascota(cita, inactiva)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactiva");

        Mascota sinAutorizador = otraMascota("Huerfana", dueno);
        dueno.setEstado(false);
        dueno.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        apoderadoRepository.saveAndFlush(dueno);
        assertThatThrownBy(() -> citaService.actualizarCita(cita.getId(), edicionConOtraMascota(cita, sinAutorizador)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene un propietario activo");
    }

    @Test
    @DisplayName("[CP-RF19-02] No se inicia la atención de una mascota sin propietario activo que autorice")
    void iniciarAtencionExigeUnPropietarioActivo() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, LocalDateTime.now().minusMinutes(20));
        Apoderado dueno = cita.getMascota().getApoderado();
        dueno.setEstado(false);
        dueno.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);
        apoderadoRepository.saveAndFlush(dueno);

        assertThatThrownBy(() -> citaService.iniciarAtencion(cita.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene un propietario activo");
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
    }

    private Cita crearCita(EstadoCita estado, LocalDateTime fechaInicio) {
        Company company = new Company();
        company.setName("VargasVet Citas");
        company.setSlug("vargasvet-citas-" + UUID.randomUUID());
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setCompany(company);
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

    private Throwable ejecutarAltaConcurrente(TransactionTemplate transaction,
                                               CitaRequest request,
                                               CountDownLatch inicio) {
        try {
            autenticarSuperAdmin();
            inicio.await();
            transaction.executeWithoutResult(status -> citaService.createCita(request));
            return null;
        } catch (Throwable error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
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
        usuario.setUsername(prefix + "-" + UUID.randomUUID());
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private void autenticarSuperAdmin() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        var principal = new veterinaria.vargasvet.security.UsuarioPrincipal(
                1, "doctor@vargasvet.test", "", authorities, null, 1,
                veterinaria.vargasvet.domain.enums.RoleScope.PLATFORM,
                veterinaria.vargasvet.domain.enums.RolePurpose.PLATFORM_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities)
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
