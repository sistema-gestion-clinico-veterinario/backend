package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.EntregaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.EstadoEntregaAviso;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.SolicitudCancelacionDatos;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaContacto;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.CanalSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.response.SolicitudCancelacionResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.SolicitudCancelacionDatosRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(CancelacionDatosService.class)
class CancelacionDatosServiceIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private CancelacionDatosService servicio;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UsuarioEmpresaContactoRepository contactoRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private SolicitudCancelacionDatosRepository solicitudRepository;

    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private PetOwnershipService petOwnershipService;
    @MockitoBean private SessionSecurityService sessionSecurityService;
    @MockitoBean private CompanyMembershipService companyMembershipService;
    @MockitoBean private VerificationCodeService verificationCodeService;
    @MockitoBean private SharedRateLimitService sharedRateLimitService;
    @MockitoBean private EmailService emailService;
    @MockitoBean private AuditLogService auditLogService;

    private static final String CORREO = "ana.torres@correo.test";
    private static final String NOMBRE = "Ana Torres";
    private static final String DNI = "45871236";

    private Company clinica;
    private Usuario administrador;
    private Usuario ana;
    private Apoderado apoderadaAna;
    private Mascota luna;
    private Cita atencionPasada;

    @BeforeEach
    void datos() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");
        clinica = empresa("Clínica Patitas");
        administrador = usuario("admin", "Admin", "Clinica", "10000001", clinica);
        ana = usuario("ana.torres", "Ana", "Torres", DNI, clinica);
        ana.setEmail(CORREO);
        em.persist(ana);
        apoderadaAna = apoderado(ana, clinica, "45871236");
        contacto(ana, clinica, "999111222", "Av. Los Olivos 123");
        credencial(ana, clinica, true);
        luna = mascota("Luna", apoderadaAna);
        atencionPasada = cita(luna, apoderadaAna, EstadoCita.COMPLETADA, -30, true);
        auditoria("LOGIN_EXITOSO", CORREO, "Inicio de sesión de " + CORREO, clinica.getId());
        auditoria("CERRAR_CUENTA", CORREO, "El usuario cerró su cuenta: " + NOMBRE + " (" + CORREO + ")", clinica.getId());
        auditoria("CREAR_MASCOTA", "admin@patitas.test", "Se registró a Luna, mascota de " + NOMBRE + " con DNI " + DNI, clinica.getId());
        em.flush();
        entrarComoAdministrador(administrador, clinica.getId());
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ cancelar y conservar

    @Test
    void laCancelacionRetiraLosDatosPersonalesPeroConservaLosRegistrosClinicos() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);

        SolicitudCancelacionResponse respuesta = servicio.ejecutar(pedido.getId());
        em.flush();
        em.clear();

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitudCancelacion.EJECUTADA);
        Usuario guardado = usuarioRepository.findById(ana.getId()).orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Titular");
        assertThat(guardado.getApellido()).isEqualTo("anonimizado");
        assertThat(guardado.getDni()).isNull();
        assertThat(guardado.getEmail()).isEqualTo("titular-" + ana.getId() + "@anonimizado.invalid");
        assertThat(guardado.getUsername()).isEqualTo("titular-" + ana.getId());
        assertThat(guardado.isActivo()).isFalse();
        Apoderado cliente = apoderadoRepository.findById(apoderadaAna.getId()).orElseThrow();
        assertThat(cliente.getNumeroDocumento()).isEqualTo("ANON-" + apoderadaAna.getId());
        assertThat(cliente.getEstado()).isFalse();
        UsuarioEmpresaContacto contacto = contactoRepository.findByUsuarioIdAndCompanyId(ana.getId(), clinica.getId()).orElseThrow();
        assertThat(contacto.getTelefono()).isNull();
        assertThat(contacto.getDireccion()).isNull();
    }

    @Test
    void lasMascotasYLasAtencionesSiguenIntactasYApuntandoAlMismoRegistro() {
        long mascotasAntes = mascotaRepository.count();
        long citasAntes = citaRepository.count();

        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());
        em.flush();
        em.clear();

        assertThat(mascotaRepository.count()).isEqualTo(mascotasAntes);
        assertThat(citaRepository.count()).isEqualTo(citasAntes);
        Cita cita = citaRepository.findById(atencionPasada.getId()).orElseThrow();
        assertThat(cita.getMascota().getId()).isEqualTo(luna.getId());
        assertThat(cita.getMascota().getApoderado().getId()).isEqualTo(apoderadaAna.getId());
        assertThat(cita.getMascota().getNombreCompleto()).isEqualTo("Luna");
    }

    @Test
    void lasCredencialesYLasSesionesDelTitularDejanDeFuncionar() {
        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());
        em.flush();
        em.clear();

        UsuarioEmpresaCredencial credencial = credencialRepository.findByUsuarioIdAndCompanyId(ana.getId(), clinica.getId()).orElseThrow();
        assertThat(credencial.getPassword()).isEqualTo("hash-nuevo");
        assertThat(credencial.isPasswordChanged()).isFalse();
        verify(sessionSecurityService).invalidateSessionsForCompany(any(Usuario.class), any(Company.class));
    }

    @Test
    void laAuditoriaConservaSusRegistrosPeroSinElCorreoNiElNombreNiElDocumento() {
        long registrosAntes = auditLogRepository.count();

        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());
        em.flush();
        em.clear();

        List<AuditLog> registros = auditLogRepository.findAll();
        assertThat(registros).hasSize((int) registrosAntes);
        String todo = registros.stream().map(r -> r.getUserEmail() + " " + r.getDetails()).reduce("", String::concat);
        assertThat(todo).doesNotContain(CORREO).doesNotContain(NOMBRE).doesNotContain(DNI);
        assertThat(registros).anySatisfy(r -> {
            assertThat(r.getAction()).isEqualTo("LOGIN_EXITOSO");
            assertThat(r.getUserEmail()).isEqualTo("titular-" + ana.getId() + "@anonimizado.invalid");
        });
        assertThat(registros).anySatisfy(r -> assertThat(r.getDetails()).contains("Titular " + ana.getId()));
    }

    @Test
    void laConstanciaDeLaCancelacionNoVuelveAGuardarDatosPersonales() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);

        servicio.ejecutar(pedido.getId());

        org.mockito.ArgumentCaptor<String> detalle = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(clinica.getId()), eq("EJECUTAR_CANCELACION_DATOS"), eq("Privacidad"), detalle.capture());
        assertThat(detalle.getValue()).contains("T-" + ana.getId())
                .doesNotContain(CORREO).doesNotContain(NOMBRE).doesNotContain(DNI);
        assertThat(solicitudRepository.findById(pedido.getId()).orElseThrow().getResumen())
                .doesNotContain(CORREO).doesNotContain(NOMBRE);
    }

    @Test
    void losEnviosDelAvisoDePrivacidadPierdenElCorreoDelTitular() {
        AvisoPrivacidad aviso = new AvisoPrivacidad();
        aviso.setCompany(clinica);
        aviso.setAudiencia(AudienciaAvisoPrivacidad.values()[0]);
        aviso.setVersion(1);
        aviso.setContenido("Aviso");
        aviso.setContenidoHash("hash");
        aviso.setCampos("{}");
        aviso.setVigenteDesde(LocalDateTime.now());
        aviso.setActivo(true);
        aviso.setCreadoEn(LocalDateTime.now());
        em.persist(aviso);
        EntregaAvisoPrivacidad entrega = new EntregaAvisoPrivacidad();
        entrega.setCompany(clinica);
        entrega.setUsuario(ana);
        entrega.setAviso(aviso);
        entrega.setCanal("CORREO");
        entrega.setDestino(CORREO);
        entrega.setEstado(EstadoEntregaAviso.PENDIENTE);
        entrega.setSolicitadoEn(LocalDateTime.now());
        entrega.setDetalle("Enviado a " + CORREO);
        em.persist(entrega);
        em.flush();

        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());
        em.flush();
        em.clear();

        EntregaAvisoPrivacidad guardada = em.find(EntregaAvisoPrivacidad.class, entrega.getId());
        assertThat(guardada.getDestino()).isEqualTo("titular-" + ana.getId() + "@anonimizado.invalid");
        assertThat(guardada.getDetalle()).isNull();
    }

    @Test
    void laSolicitudQuedaResueltaConQuienYCuando() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);

        servicio.ejecutar(pedido.getId());
        em.flush();
        em.clear();

        SolicitudCancelacionDatos guardada = solicitudRepository.findById(pedido.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudCancelacion.EJECUTADA);
        assertThat(guardada.getResueltaPorUsuarioId()).isEqualTo(administrador.getId());
        assertThat(guardada.getResueltaAt()).isNotNull();
        assertThat(guardada.getMotivoTitular()).isNull();
    }

    @Test
    void avisaAlTitularPorCorreoAntesDeRetirarSuDireccion() {
        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());

        verify(emailService).createMail(eq(CORREO), anyString(), any());
        verify(emailService).sendEmailWithRetry(any(), eq("email/data-cancellation-executed-template"));
    }

    // ------------------------------------------------------------------ identidad compartida

    @Test
    void siLaPersonaSigueSiendoClienteDeOtraClinicaSoloSeCancelaLoDeEstaClinica() {
        Company otra = empresa("Veterinaria Norte");
        apoderado(ana, otra, "45871236");
        contacto(ana, otra, "988777666", "Jr. Norte 45");
        em.flush();

        servicio.ejecutar(solicitudPendiente(ana, clinica).getId());
        em.flush();
        em.clear();

        Usuario guardado = usuarioRepository.findById(ana.getId()).orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getEmail()).isEqualTo(CORREO);
        assertThat(apoderadoRepository.findById(apoderadaAna.getId()).orElseThrow().getNumeroDocumento())
                .startsWith("ANON-");
        assertThat(contactoRepository.findByUsuarioIdAndCompanyId(ana.getId(), otra.getId()).orElseThrow().getTelefono())
                .isEqualTo("988777666");
        String todo = auditLogRepository.findAll().stream().map(r -> r.getUserEmail() + " " + r.getDetails())
                .reduce("", String::concat);
        assertThat(todo).doesNotContain(CORREO);
    }

    // ------------------------------------------------------------------ cuándo no se puede ejecutar

    @Test
    void conUnaCitaProgramadaNoSeEjecutaYNoSeCambiaNada() {
        cita(luna, apoderadaAna, EstadoCita.PROGRAMADA, 3, true);
        em.flush();
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);

        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("citas programadas");
        em.flush();
        em.clear();

        assertThat(usuarioRepository.findById(ana.getId()).orElseThrow().getEmail()).isEqualTo(CORREO);
        assertThat(solicitudRepository.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoSolicitudCancelacion.PENDIENTE);
    }

    @Test
    void conUnaDeudaPendienteLaClinicaDebeConservarSusDatos() {
        cita(luna, apoderadaAna, EstadoCita.COMPLETADA, -2, false);
        em.flush();

        assertThatThrownBy(() -> servicio.ejecutar(solicitudPendiente(ana, clinica).getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("deuda pendiente");
    }

    @Test
    void elPersonalActivoDeLaClinicaNoSeCancelaPorEstaVia() {
        Empleado empleado = new Empleado();
        empleado.setUser(ana);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad("45871236");
        empleado.setGenero(Genero.FEMENINO);
        empleado.setEstado(true);
        empleado.setCompany(clinica);
        em.persist(empleado);
        em.flush();

        assertThatThrownBy(() -> servicio.ejecutar(solicitudPendiente(ana, clinica).getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("personal");
    }

    @Test
    void noSeEjecutaDosVecesSobreLaMismaSolicitud() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);
        servicio.ejecutar(pedido.getId());

        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ya fue resuelta");
    }

    @Test
    void sinIdentidadVerificadaNoSeEjecuta() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);
        pedido.setIdentidadVerificada(false);
        em.flush();

        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("verificar la identidad");
    }

    @Test
    void elAdministradorNoPuedeResolverUnaSolicitudSobreSusPropiosDatos() {
        Apoderado propio = apoderado(administrador, clinica, "10000001");
        em.flush();
        SolicitudCancelacionDatos pedido = solicitudPendiente(administrador, clinica);

        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(propio.getNumeroDocumento()).doesNotStartWith("ANON-");
    }

    // ------------------------------------------------------------------ rechazo

    @Test
    void rechazarExigeUnMotivoYNoToscaLosDatos() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);

        assertThatThrownBy(() -> servicio.rechazar(pedido.getId(), "corto"))
                .isInstanceOf(IllegalArgumentException.class);
        SolicitudCancelacionResponse respuesta = servicio.rechazar(pedido.getId(), "Tiene una obligación legal de conservación vigente");
        em.flush();
        em.clear();

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitudCancelacion.RECHAZADA);
        assertThat(respuesta.motivoResolucion()).contains("obligación legal");
        assertThat(usuarioRepository.findById(ana.getId()).orElseThrow().getEmail()).isEqualTo(CORREO);
        verify(emailService).sendEmailWithRetry(any(), eq("email/data-cancellation-rejected-template"));
        verify(auditLogService).log(eq(clinica.getId()), eq("RECHAZAR_CANCELACION_DATOS"), eq("Privacidad"), contains("obligación legal"));
    }

    // ------------------------------------------------------------------ permisos y aislamiento

    @Test
    void soloElAdministradorDeLaClinicaGestionaLasSolicitudes() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);

        assertThatThrownBy(() -> servicio.listar(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.rechazar(pedido.getId(), "Motivo suficientemente largo"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unaClinicaNoVeNiResuelveLasSolicitudesDeOtra() {
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);
        Company otra = empresa("Veterinaria Norte");
        Usuario otroAdmin = usuario("admin.norte", "Admin", "Norte", "10000002", otra);
        em.flush();
        entrarComoAdministrador(otroAdmin, otra.getId());

        assertThat(servicio.listar(null)).isEmpty();
        assertThatThrownBy(() -> servicio.ejecutar(pedido.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio.rechazar(pedido.getId(), "Motivo suficientemente largo"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elListadoMuestraAlAdministradorQuienEsElTitularYSiPuedeEjecutarse() {
        solicitudPendiente(ana, clinica);

        List<SolicitudCancelacionResponse> lista = servicio.listar(EstadoSolicitudCancelacion.PENDIENTE);

        assertThat(lista).singleElement().satisfies(s -> {
            assertThat(s.titularNombre()).isEqualTo(NOMBRE);
            assertThat(s.titularCorreo()).isEqualTo(CORREO);
            assertThat(s.puedeEjecutarse()).isTrue();
            assertThat(s.impedimento()).isNull();
            assertThat(s.vencida()).isFalse();
        });
    }

    @Test
    void elListadoAvisaCuandoHayUnImpedimentoOElPlazoVencio() {
        cita(luna, apoderadaAna, EstadoCita.PROGRAMADA, 3, true);
        SolicitudCancelacionDatos pedido = solicitudPendiente(ana, clinica);
        pedido.setFechaLimite(java.time.LocalDate.now().minusDays(1));
        em.flush();

        assertThat(servicio.listar(null)).singleElement().satisfies(s -> {
            assertThat(s.puedeEjecutarse()).isFalse();
            assertThat(s.impedimento()).contains("citas programadas");
            assertThat(s.vencida()).isTrue();
        });
    }

    // ------------------------------------------------------------------ solicitud del titular

    @Test
    void elTitularPideElCodigoConSuContrasenaYSeRegistra() {
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);
        when(passwordEncoder.matches("Correcta!1", "hash")).thenReturn(true);
        when(verificationCodeService.issue(any(), any(), eq("CANCELACION_DATOS"))).thenReturn("123456");
        when(verificationCodeService.segundosParaPedirOtro(any(), any(), anyString())).thenReturn(60L);

        long espera = servicio.pedirCodigo("Correcta!1");

        assertThat(espera).isEqualTo(60L);
        verify(emailService).sendEmailWithRetry(any(), eq("email/data-cancellation-code-template"));
    }

    @Test
    void conUnaContrasenaIncorrectaNoSeEnviaElCodigo() {
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> servicio.pedirCodigo("Equivocada"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("contraseña");
        verify(verificationCodeService, never()).issue(any(), any(), anyString());
    }

    @Test
    void laSolicitudDelTitularQuedaVerificadaConPlazoDeRespuesta() {
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);

        SolicitudCancelacionResponse respuesta = servicio.solicitar("123456", "Ya no soy cliente");
        em.flush();

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitudCancelacion.PENDIENTE);
        assertThat(respuesta.canal()).isEqualTo(CanalSolicitudCancelacion.TITULAR);
        assertThat(respuesta.identidadVerificada()).isTrue();
        assertThat(respuesta.metodoVerificacion()).contains("código");
        assertThat(respuesta.fechaLimite()).isAfter(java.time.LocalDate.now());
        verify(verificationCodeService).verifyAndConsume(eq(ana.getId()), eq(clinica.getId()), eq("CANCELACION_DATOS"), eq("123456"));
        verify(auditLogService).log(eq(clinica.getId()), eq("SOLICITAR_CANCELACION_DATOS"), eq("Privacidad"), contains("T-" + ana.getId()));
    }

    @Test
    void unCodigoInvalidoNoCreaSolicitud() {
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Código incorrecto"))
                .when(verificationCodeService).verifyAndConsume(anyInt(), anyInt(), anyString(), eq("000000"));

        assertThatThrownBy(() -> servicio.solicitar("000000", null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(solicitudRepository.count()).isZero();
    }

    @Test
    void noSePuedeTenerDosSolicitudesEnRevisionALaVez() {
        solicitudPendiente(ana, clinica);
        entrarComo(ana, clinica.getId(), RolePurpose.CLIENT_PORTAL);

        assertThatThrownBy(() -> servicio.solicitar("123456", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("en revisión");
        assertThat(servicio.elegibilidad().puedeSolicitar()).isFalse();
    }

    @Test
    void elPersonalNoPuedeSolicitarloDesdeSuCuentaDeCliente() {
        entrarComo(administrador, clinica.getId(), RolePurpose.COMPANY_ADMIN);

        assertThatThrownBy(() -> servicio.pedirCodigo("x")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> servicio.elegibilidad()).isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------ registro por el administrador

    @Test
    void elAdministradorPuedeRegistrarLaSolicitudDeQuienYaNoTieneAcceso() {
        SolicitudCancelacionResponse respuesta = servicio.registrarPorAdministrador(
                apoderadaAna.getId(), "Presentó su DNI en el mostrador", "Pide eliminar sus datos");
        em.flush();

        assertThat(respuesta.canal()).isEqualTo(CanalSolicitudCancelacion.ADMINISTRADOR);
        assertThat(respuesta.identidadVerificada()).isTrue();
        assertThat(respuesta.metodoVerificacion()).contains("Presentó su DNI");
        assertThat(solicitudRepository.findFirstByUsuarioIdAndCompanyIdOrderByIdDesc(ana.getId(), clinica.getId()))
                .get().extracting(SolicitudCancelacionDatos::getRegistradaPorUsuarioId).isEqualTo(administrador.getId());
    }

    @Test
    void noSePuedeRegistrarLaSolicitudDeUnClienteDeOtraClinica() {
        Company otra = empresa("Veterinaria Norte");
        Usuario cliente = usuario("beto", "Beto", "Ruiz", "20000001", otra);
        Apoderado ajeno = apoderado(cliente, otra, "20000001");
        em.flush();

        assertThatThrownBy(() -> servicio.registrarPorAdministrador(ajeno.getId(), "Presentó su DNI", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nuncaQuedanDosSolicitudesEnRevisionDeLaMismaPersona() {
        servicio.registrarPorAdministrador(apoderadaAna.getId(), "Presentó su DNI", null);
        em.flush();

        assertThatThrownBy(() -> servicio.registrarPorAdministrador(apoderadaAna.getId(), "Presentó su DNI", null))
                .isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------ plazo

    @Test
    void elPlazoDeRespuestaCuentaSoloDiasHabiles() {
        java.time.LocalDate viernes = java.time.LocalDate.of(2026, 10, 9);

        assertThat(CancelacionDatosService.sumarDiasHabiles(viernes, 1)).isEqualTo(java.time.LocalDate.of(2026, 10, 12));
        assertThat(CancelacionDatosService.sumarDiasHabiles(viernes, 10)).isEqualTo(java.time.LocalDate.of(2026, 10, 23));
        assertThat(CancelacionDatosService.sumarDiasHabiles(java.time.LocalDate.of(2026, 10, 10), 1))
                .isEqualTo(java.time.LocalDate.of(2026, 10, 12));
    }

    // ------------------------------------------------------------------ datos de prueba

    private Company empresa(String nombre) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug("clinica-" + UUID.randomUUID());
        company.setRuc(digitos(11));
        company.setActivo(true);
        return em.persist(company);
    }

    private Usuario usuario(String prefijo, String nombre, String apellido, String dni, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername(prefijo + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setDni(dni);
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return em.persist(usuario);
    }

    private Apoderado apoderado(Usuario usuario, Company company, String documento) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(documento);
        apoderado.setGenero(Genero.FEMENINO);
        apoderado.setReferencias("Referida por una vecina");
        apoderado.setObservaciones("Prefiere llamadas por la tarde");
        return em.persist(apoderado);
    }

    private void contacto(Usuario usuario, Company company, String telefono, String direccion) {
        UsuarioEmpresaContacto contacto = new UsuarioEmpresaContacto();
        contacto.setUsuario(usuario);
        contacto.setCompany(company);
        contacto.setTelefono(telefono);
        contacto.setDireccion(direccion);
        contacto.setCreatedAt(LocalDateTime.now());
        contacto.setUpdatedAt(LocalDateTime.now());
        em.persist(contacto);
    }

    private void credencial(Usuario usuario, Company company, boolean conContrasena) {
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setCompany(company);
        credencial.setPassword("hash");
        credencial.setPasswordChanged(conContrasena);
        credencial.setCreatedAt(LocalDateTime.now());
        credencial.setUpdatedAt(LocalDateTime.now());
        em.persist(credencial);
    }

    private Mascota mascota(String nombre, Apoderado dueno) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        return em.persist(mascota);
    }

    private Cita cita(Mascota mascota, Apoderado dueno, EstadoCita estado, int diasDesdeHoy, boolean pagada) {
        Usuario veterinario = usuario("vet", "Vera", "Vet", digitos(8), dueno.getCompany());
        Empleado empleado = new Empleado();
        empleado.setUser(veterinario);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(digitos(8));
        empleado.setGenero(Genero.FEMENINO);
        empleado.setEstado(true);
        empleado.setCompany(dueno.getCompany());
        em.persist(empleado);
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(dueno.getCompany());
        servicio.setNombre("Consulta " + UUID.randomUUID());
        servicio.setDescripcion("Consulta veterinaria general");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        em.persist(servicio);
        LocalDateTime inicio = LocalDateTime.now().plusDays(diasDesdeHoy);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(pagada ? new BigDecimal("100.00") : BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return em.persist(cita);
    }

    private void auditoria(String accion, String correo, String detalle, Integer companyId) {
        em.persist(AuditLog.builder().timestamp(LocalDateTime.now()).userEmail(correo).action(accion)
                .module("Seguridad").details(detalle).companyId(companyId).build());
    }

    private SolicitudCancelacionDatos solicitudPendiente(Usuario titular, Company company) {
        SolicitudCancelacionDatos solicitud = new SolicitudCancelacionDatos();
        solicitud.setUsuario(titular);
        solicitud.setCompany(company);
        solicitud.setCanal(CanalSolicitudCancelacion.TITULAR);
        solicitud.setEstado(EstadoSolicitudCancelacion.PENDIENTE);
        solicitud.setIdentidadVerificada(true);
        solicitud.setMetodoVerificacion("Contraseña y código enviado al correo");
        solicitud.setMotivoTitular("Ya no uso el servicio");
        solicitud.setCreadaAt(LocalDateTime.now());
        solicitud.setFechaLimite(java.time.LocalDate.now().plusDays(14));
        em.persist(solicitud);
        em.flush();
        return solicitud;
    }

    private void entrarComoAdministrador(Usuario usuario, Integer companyId) {
        entrarComo(usuario, companyId, RolePurpose.COMPANY_ADMIN);
    }

    private void entrarComo(Usuario usuario, Integer companyId, RolePurpose proposito) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                usuario.getId(), usuario.getEmail(), "n/a", List.of(), companyId, 1, null, proposito, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private String digitos(int largo) {
        String valor = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (valor.length() < largo) {
            valor += "0";
        }
        return valor.substring(0, largo);
    }
}
