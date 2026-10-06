package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.CierreCuenta;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.response.AccountClosureEligibility;
import veterinaria.vargasvet.exception.InvalidVerificationCodeException;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CierreCuentaRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountClosureServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock CierreCuentaRepository cierreCuentaRepository;
    @Mock SesionCajaRepository sesionCajaRepository;
    @Mock CitaRepository citaRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AdministratorProtection administratorProtection;
    @Mock PetOwnershipService petOwnershipService;
    @Mock SessionSecurityService sessionSecurityService;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock VerificationCodeService verificationCodeService;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;

    private AccountClosureService service;
    private Usuario usuario;
    private Company company;
    private UsuarioEmpresaCredencial credencial;
    private Empleado empleado;
    private Apoderado apoderado;

    @BeforeEach
    void setUp() {
        service = new AccountClosureService(usuarioRepository, companyRepository, empleadoRepository, apoderadoRepository,
                credencialRepository, cierreCuentaRepository, sesionCajaRepository, citaRepository,
                passwordResetTokenRepository, passwordEncoder, administratorProtection, petOwnershipService,
                sessionSecurityService, companyMembershipService, verificationCodeService, sharedRateLimitService,
                emailService, auditLogService);
        ReflectionTestUtils.setField(service, "graceDays", 30L);
        ReflectionTestUtils.setField(service, "codeValidityMinutes", 10L);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.test");
        ReflectionTestUtils.setField(service, "defaultCompanyName", "SoftVet");
        ReflectionTestUtils.setField(service, "defaultCompanyLogo", "https://cdn.test/default.png");
        ReflectionTestUtils.setField(service, "defaultCompanyEmail", "soporte@softvet.test");
        ReflectionTestUtils.setField(service, "defaultCompanyPhone", "+51 000 000 000");
        ReflectionTestUtils.setField(service, "defaultCompanyAddress", "Lima");

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("ana@example.test");
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        credencial = new UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setCompany(company);
        credencial.setPassword("hash-actual");
        credencial.setPasswordChanged(true);

        lenient().when(usuarioRepository.findById(10)).thenReturn(Optional.of(usuario));
        lenient().when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        lenient().when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        lenient().when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(10, 7)).thenReturn(Optional.empty());
        lenient().when(apoderadoRepository.findByUserIdAndCompanyId(10, 7)).thenReturn(Optional.empty());
        lenient().when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA))
                .thenReturn(List.of());
        lenient().when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void sesion(RolePurpose purpose, Integer companyId) {
        RoleScope scope = purpose == RolePurpose.PLATFORM_ADMIN ? RoleScope.PLATFORM
                : purpose == RolePurpose.CLIENT_PORTAL ? RoleScope.CLIENT : RoleScope.STAFF;
        UsuarioPrincipal principal = new UsuarioPrincipal(10, "ana@example.test", "", List.of(), companyId, 2, scope, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Apoderado cliente() {
        apoderado = new Apoderado();
        apoderado.setId(40L);
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setEstado(true);
        when(apoderadoRepository.findByUserIdAndCompanyId(10, 7)).thenReturn(Optional.of(apoderado));
        return apoderado;
    }

    private Empleado administrador() {
        empleado = new Empleado();
        empleado.setId(30L);
        empleado.setUser(usuario);
        empleado.setCompany(company);
        empleado.setEstado(true);
        when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(10, 7)).thenReturn(Optional.of(empleado));
        when(administratorProtection.isAdministrator(usuario, 7)).thenReturn(true);
        return empleado;
    }

    // ---- elegibilidad

    @Test
    void unClienteSinCitasPuedeCerrarSuCuenta() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isTrue();
        assertThat(resultado.requiresPassword()).isTrue();
    }

    @Test
    void quienActivoConGoogleYNoTieneContrasenaNoNecesitaEscribirla() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        credencial.setPasswordChanged(false);

        assertThat(service.eligibility().requiresPassword()).isFalse();
    }

    @Test
    void unAdministradorQueNoEsElUnicoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();

        assertThat(service.eligibility().eligible()).isTrue();
        verify(administratorProtection).assertNotLastAdministrator(usuario, 7);
    }

    @Test
    void elUnicoAdministradorNoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        doThrow(new IllegalStateException("No puedes cerrar tu cuenta porque eres el único administrador activo"))
                .when(administratorProtection).assertNotLastAdministrator(usuario, 7);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("único administrador");
    }

    @Test
    void laCuentaDePlataformaNuncaSePuedeCerrar() {
        sesion(RolePurpose.PLATFORM_ADMIN, null);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("plataforma");
        assertThatThrownBy(() -> service.requestClosure("x")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.confirmClosure("123456")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(verificationCodeService, cierreCuentaRepository);
    }

    @Test
    void unEmpleadoConUnRolDeTrabajoNoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.CUSTOM, 7);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("administrador");
    }

    @Test
    void quienEsClienteYTambienTienePuestoNoAdministradorNoPuedeCerrar() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        empleado = new Empleado();
        empleado.setId(30L);
        empleado.setEstado(true);
        when(empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(10, 7)).thenReturn(Optional.of(empleado));
        when(administratorProtection.isAdministrator(usuario, 7)).thenReturn(false);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("puesto");
    }

    @Test
    void sinRelacionActivaEnLaEmpresaNoHayNadaQueCerrar() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);

        assertThat(service.eligibility().eligible()).isFalse();
    }

    @Test
    void quienTieneLaCajaAbiertaNoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        SesionCaja caja = new SesionCaja();
        caja.setEstado(EstadoSesionCaja.ABIERTA);
        caja.setAbiertaPorUsuarioId(10);
        caja.setAbiertaPor("otro.correo@example.test");
        when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA))
                .thenReturn(List.of(caja));

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("caja abierta");
    }

    @Test
    void conVariasCajasAbiertasSeRevisanTodasYNoSoloLaPrimera() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        SesionCaja deOtra = new SesionCaja();
        deOtra.setEstado(EstadoSesionCaja.ABIERTA);
        deOtra.setAbiertaPorUsuarioId(99);
        deOtra.setAbiertaPor("otra@example.test");
        SesionCaja mia = new SesionCaja();
        mia.setEstado(EstadoSesionCaja.ABIERTA);
        mia.setAbiertaPorUsuarioId(10);
        mia.setAbiertaPor("ana@example.test");
        when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA))
                .thenReturn(List.of(deOtra, mia));

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("caja abierta");
    }

    @Test
    void unaCajaAbiertaPorOtraPersonaNoImpideElCierre() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        SesionCaja caja = new SesionCaja();
        caja.setEstado(EstadoSesionCaja.ABIERTA);
        caja.setAbiertaPorUsuarioId(99);
        caja.setAbiertaPor("ana@example.test");
        when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA))
                .thenReturn(List.of(caja));

        assertThat(service.eligibility().eligible()).isTrue();
    }

    @Test
    void unaCajaAnteriorAlRegistroPorPersonaSeReconocePorElCorreo() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        SesionCaja caja = new SesionCaja();
        caja.setEstado(EstadoSesionCaja.ABIERTA);
        caja.setAbiertaPor("ANA@example.test");
        when(sesionCajaRepository.findAllByCompanyIdAndEstado(7, EstadoSesionCaja.ABIERTA))
                .thenReturn(List.of(caja));

        assertThat(service.eligibility().eligible()).isFalse();
    }

    @Test
    void unClienteConCitasProgramadasNoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        when(citaRepository.existsCitaVigenteByApoderadoId(eq(40L), any())).thenReturn(true);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("citas programadas");
    }

    @Test
    void unAdministradorConCitasComoProfesionalNoPuedeCerrarSuCuenta() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        when(citaRepository.existsCitaVigenteByEmpleadoId(eq(30L), any())).thenReturn(true);

        AccountClosureEligibility resultado = service.eligibility();

        assertThat(resultado.eligible()).isFalse();
        assertThat(resultado.reason()).contains("como profesional");
    }

    // ---- pedir el código

    @Test
    void conContrasenaPropiaSeExigeYSiEsIncorrectaNoSeEnviaCodigo() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        when(passwordEncoder.matches("mala", "hash-actual")).thenReturn(false);

        assertThatThrownBy(() -> service.requestClosure("mala"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña es incorrecta");

        verifyNoInteractions(verificationCodeService, emailService);
    }

    @Test
    void conLaContrasenaCorrectaSeEnviaElCodigoAlCorreoDeLaPersona() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        when(passwordEncoder.matches("buena", "hash-actual")).thenReturn(true);
        when(verificationCodeService.issue(usuario, company, "CIERRE_CUENTA")).thenReturn("482913");

        service.requestClosure("buena");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(eq("ana@example.test"), anyString(), modelo.capture());
        assertThat(modelo.getValue()).containsEntry("code", "482913").containsEntry("validityMinutes", 10L);
        verify(emailService).sendEmailWithRetry(any(), eq("email/account-close-code-template"));
        verify(sharedRateLimitService).enforce("account-closure-request", "10:7", 3, Duration.ofHours(1));
        verify(sharedRateLimitService).enforce("account-closure-password", "10:7", 5, Duration.ofMinutes(15));
        verify(auditLogService).log(eq(7), eq("SOLICITAR_CIERRE_CUENTA"), eq("Seguridad"), anyString());
    }

    @Test
    void quienNoTieneContrasenaPropiaSoloNecesitaElCodigo() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        credencial.setPasswordChanged(false);
        when(verificationCodeService.issue(usuario, company, "CIERRE_CUENTA")).thenReturn("111222");

        service.requestClosure(null);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(emailService).sendEmailWithRetry(any(), eq("email/account-close-code-template"));
    }

    @Test
    void elCorreoLlevaLaMarcaDeLaClinicaDeLaPersonaYSoloUnColorValido() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        credencial.setPasswordChanged(false);
        company.setLogoUrl("https://cdn.test/patitas.png");
        company.setEmail("hola@patitas.test");
        company.setPhone("+51 999 111 222");
        company.setAddress("Av. Principal 123");
        company.setColorPrimario("#0a7d8c");
        when(verificationCodeService.issue(usuario, company, "CIERRE_CUENTA")).thenReturn("111222");

        service.requestClosure(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(anyString(), anyString(), modelo.capture());
        assertThat(modelo.getValue())
                .containsEntry("companyName", "Vargas Vet")
                .containsEntry("companyLogo", "https://cdn.test/patitas.png")
                .containsEntry("companyEmail", "hola@patitas.test")
                .containsEntry("companyPhone", "+51 999 111 222")
                .containsEntry("companyAddress", "Av. Principal 123")
                .containsEntry("accentColor", "#0a7d8c");
    }

    @Test
    void unColorQueNoEsHexadecimalNoLlegaAlCorreo() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        credencial.setPasswordChanged(false);
        company.setColorPrimario("red;} body{display:none");
        when(verificationCodeService.issue(usuario, company, "CIERRE_CUENTA")).thenReturn("111222");

        service.requestClosure(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(anyString(), anyString(), modelo.capture());
        assertThat(modelo.getValue()).doesNotContainKey("accentColor");
    }

    @Test
    void sinDatosPropiosLaClinicaUsaLosDeLaPlataforma() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        credencial.setPasswordChanged(false);
        when(verificationCodeService.issue(usuario, company, "CIERRE_CUENTA")).thenReturn("111222");

        service.requestClosure(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(anyString(), anyString(), modelo.capture());
        assertThat(modelo.getValue())
                .containsEntry("companyLogo", "https://cdn.test/default.png")
                .containsEntry("companyEmail", "soporte@softvet.test");
    }

    @Test
    void siHayUnImpedimentoNoSeGastaNingunCodigo() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        when(citaRepository.existsCitaVigenteByApoderadoId(eq(40L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.requestClosure("x")).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(verificationCodeService, emailService);
    }

    // ---- confirmar el cierre

    @Test
    void cerrarLaCuentaDeUnClienteCortaElAccesoPausaSusMascotasYEnviaElCorreoConElEnlace() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();

        service.confirmClosure("482913");

        assertThat(apoderado.getEstado()).isFalse();
        assertThat(apoderado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        assertThat(apoderado.getFechaSalida()).isEqualTo(AppClock.today());
        assertThat(apoderado.getEstadoModificadoPor()).isEqualTo("ana@example.test");
        verify(petOwnershipService).syncPets(apoderado, TipoInactividad.BAJA);
        verify(companyMembershipService).syncLegacyCompanyField(usuario);
        verify(sessionSecurityService).invalidateSessionsForCredential(credencial);
        verify(passwordResetTokenRepository).deleteByUsuarioAndCompany(usuario, company);
        verify(auditLogService).log(eq(7), eq("CERRAR_CUENTA"), eq("Seguridad"), anyString());

        ArgumentCaptor<CierreCuenta> guardado = ArgumentCaptor.forClass(CierreCuenta.class);
        verify(cierreCuentaRepository).save(guardado.capture());
        CierreCuenta cierre = guardado.getValue();
        assertThat(cierre.getEstado()).isEqualTo(EstadoCierreCuenta.CERRADA);
        assertThat(cierre.getApoderadoId()).isEqualTo(40L);
        assertThat(cierre.getEmpleadoId()).isNull();
        assertThat(cierre.getVenceAt()).isBetween(AppClock.now().plusDays(30).minusMinutes(1), AppClock.now().plusDays(30).plusMinutes(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(eq("ana@example.test"), anyString(), modelo.capture());
        String enlace = (String) modelo.getValue().get("reactivateUrl");
        String token = enlace.substring(enlace.indexOf("#token=") + "#token=".length());
        assertThat(enlace).startsWith("https://app.test/vargas-vet/reactivate-account#token=");
        assertThat(cierre.getTokenHash()).isEqualTo(SecurityTokenUtils.hash(token)).isNotEqualTo(token);
        verify(emailService).sendEmailWithRetry(any(), eq("email/account-closed-template"));
    }

    @Test
    void cerrarLaCuentaDeUnAdministradorDaDeBajaSuPuestoYRecuerdaQueRelacionCerro() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();

        service.confirmClosure("482913");

        assertThat(empleado.getEstado()).isFalse();
        assertThat(empleado.getTipoInactividad()).isEqualTo(TipoInactividad.BAJA);
        ArgumentCaptor<CierreCuenta> guardado = ArgumentCaptor.forClass(CierreCuenta.class);
        verify(cierreCuentaRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEmpleadoId()).isEqualTo(30L);
        verify(petOwnershipService, never()).syncPets(any(), any());
    }

    @Test
    void losImpedimentosSeRevisanAntesDeGastarElCodigo() {
        sesion(RolePurpose.COMPANY_ADMIN, 7);
        administrador();
        doThrow(new IllegalStateException("único administrador"))
                .when(administratorProtection).assertNotLastAdministrator(usuario, 7);

        assertThatThrownBy(() -> service.confirmClosure("482913")).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(verificationCodeService);
        verify(cierreCuentaRepository, never()).save(any());
    }

    @Test
    void conUnCodigoIncorrectoNoSeCierraNada() {
        sesion(RolePurpose.CLIENT_PORTAL, 7);
        cliente();
        doThrow(new InvalidVerificationCodeException("El código no es correcto."))
                .when(verificationCodeService).verifyAndConsume(10, 7, "CIERRE_CUENTA", "000000");

        assertThatThrownBy(() -> service.confirmClosure("000000")).isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(apoderado.getEstado()).isTrue();
        verify(cierreCuentaRepository, never()).save(any());
        verifyNoInteractions(sessionSecurityService, petOwnershipService, emailService);
    }

    // ---- reactivar

    private CierreCuenta cierreAbierto(String token) {
        CierreCuenta cierre = new CierreCuenta();
        cierre.setUsuario(usuario);
        cierre.setCompany(company);
        cierre.setEstado(EstadoCierreCuenta.CERRADA);
        cierre.setTokenHash(SecurityTokenUtils.hash(token));
        cierre.setCerradaAt(AppClock.now().minusDays(3));
        cierre.setVenceAt(AppClock.now().plusDays(27));
        return cierre;
    }

    @Test
    void elEnlaceDelCorreoRestauraLaRelacionYLasMascotasPausadas() {
        CierreCuenta cierre = cierreAbierto("token-bueno");
        cierre.setApoderadoId(40L);
        cierre.setEmpleadoId(30L);
        Apoderado cerrado = new Apoderado();
        cerrado.setId(40L);
        cerrado.setEstado(false);
        cerrado.setTipoInactividad(TipoInactividad.BAJA);
        cerrado.setFechaSalida(AppClock.today());
        Empleado puesto = new Empleado();
        puesto.setId(30L);
        puesto.setEstado(false);
        puesto.setTipoInactividad(TipoInactividad.BAJA);
        when(cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash("token-bueno"))).thenReturn(Optional.of(cierre));
        when(apoderadoRepository.findById(40L)).thenReturn(Optional.of(cerrado));
        when(empleadoRepository.findById(30L)).thenReturn(Optional.of(puesto));

        service.reactivate("token-bueno");

        assertThat(cerrado.getEstado()).isTrue();
        assertThat(cerrado.getTipoInactividad()).isNull();
        assertThat(cerrado.getFechaSalida()).isNull();
        assertThat(puesto.getEstado()).isTrue();
        assertThat(puesto.getTipoInactividad()).isNull();
        assertThat(cierre.getEstado()).isEqualTo(EstadoCierreCuenta.REACTIVADA);
        assertThat(cierre.getReactivadaAt()).isNotNull();
        verify(petOwnershipService).syncPets(cerrado, null);
        verify(companyMembershipService).syncLegacyCompanyField(usuario);
        verify(emailService).sendEmailWithRetry(any(), eq("email/account-reactivated-template"));
        verify(auditLogService).log(eq("ana@example.test"), eq("USER"), eq(7), eq("Vargas Vet"), eq("REACTIVAR_CUENTA"),
                eq("Seguridad"), anyString(), any());
    }

    private CierreCuenta cierreDelDuenoConApoderado(Boolean estado, TipoInactividad tipo) {
        usuario.setActivo(true);
        CierreCuenta cierre = cierreAbierto("sin-enlace");
        cierre.setApoderadoId(40L);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(40L);
        apoderado.setEstado(estado);
        apoderado.setTipoInactividad(tipo);
        lenient().when(apoderadoRepository.findById(40L)).thenReturn(Optional.of(apoderado));
        lenient().when(cierreCuentaRepository.findByUsuarioAndCompanyAndEstadoForUpdate(10, 7, EstadoCierreCuenta.CERRADA))
                .thenReturn(List.of(cierre));
        return cierre;
    }

    @Test
    void quienCerroSuCuentaPuedeReactivarlaSinElEnlaceDelCorreo() {
        CierreCuenta cierre = cierreDelDuenoConApoderado(false, TipoInactividad.BAJA);

        service.reactivateOwn(10, 7);

        assertThat(cierre.getEstado()).isEqualTo(EstadoCierreCuenta.REACTIVADA);
        verify(petOwnershipService).syncPets(any(Apoderado.class), eq(null));
        verify(emailService).sendEmailWithRetry(any(), eq("email/account-reactivated-template"));
    }

    @Test
    void siLaClinicaSuspendioElAccesoDespuesDelCierreNoSeReactivaIniciandoSesion() {
        CierreCuenta cierre = cierreDelDuenoConApoderado(false, TipoInactividad.SUSPENSION);

        assertThatThrownBy(() -> service.reactivateOwn(10, 7))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("lo gestiona el administrador");

        assertThat(cierre.getEstado()).isEqualTo(EstadoCierreCuenta.CERRADA);
        verifyNoInteractions(petOwnershipService, emailService);
    }

    @Test
    void siElUsuarioFueDesactivadoNoSeReactiva() {
        CierreCuenta cierre = cierreDelDuenoConApoderado(false, TipoInactividad.BAJA);
        usuario.setActivo(false);

        assertThatThrownBy(() -> service.reactivateOwn(10, 7))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(cierre.getEstado()).isEqualTo(EstadoCierreCuenta.CERRADA);
    }

    @Test
    void sinUnCierreVigenteNoHayNadaQueReactivarPorLaCuenta() {
        when(cierreCuentaRepository.findByUsuarioAndCompanyAndEstadoForUpdate(10, 7, EstadoCierreCuenta.CERRADA))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.reactivateOwn(10, 7)).isInstanceOf(ResourceNotFoundException.class);

        CierreCuenta vencido = cierreDelDuenoConApoderado(false, TipoInactividad.BAJA);
        vencido.setVenceAt(AppClock.now().minusMinutes(1));

        assertThatThrownBy(() -> service.reactivateOwn(10, 7)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unEnlaceVencidoOQueNoExisteNoReactivaNada() {
        CierreCuenta vencido = cierreAbierto("token-viejo");
        vencido.setVenceAt(AppClock.now().minusMinutes(1));
        when(cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash("token-viejo"))).thenReturn(Optional.of(vencido));
        when(cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash("desconocido"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reactivate("token-viejo")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.reactivate("desconocido")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.reactivate("  ")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.reactivate(null)).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(petOwnershipService, emailService);
    }

    @Test
    void elEnlaceNoSirveDosVecesNiDespuesDeLaPurga() {
        CierreCuenta usado = cierreAbierto("token-usado");
        usado.setEstado(EstadoCierreCuenta.REACTIVADA);
        CierreCuenta purgado = cierreAbierto("token-purgado");
        purgado.setEstado(EstadoCierreCuenta.PURGADA);
        when(cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash("token-usado"))).thenReturn(Optional.of(usado));
        when(cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash("token-purgado"))).thenReturn(Optional.of(purgado));

        assertThatThrownBy(() -> service.reactivate("token-usado")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.reactivate("token-purgado")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- purga

    @Test
    void vencidoElPlazoSeBorraElSecretoDeLaCredencialSoloDeEsaClinica() {
        CierreCuenta vencido = cierreAbierto("t");
        vencido.setVenceAt(AppClock.now().minusDays(1));
        credencial.setActivatedWithGoogle(true);
        credencial.setCredentialsVersion(4L);
        when(cierreCuentaRepository.findTop100ByEstadoAndVenceAtBeforeOrderByVenceAtAsc(eq(EstadoCierreCuenta.CERRADA), any()))
                .thenReturn(List.of(vencido));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio-nuevo");

        int purgadas = service.purgeExpired();

        assertThat(purgadas).isEqualTo(1);
        assertThat(credencial.getPassword()).isEqualTo("hash-aleatorio-nuevo");
        assertThat(credencial.isPasswordChanged()).isFalse();
        assertThat(credencial.isActivatedWithGoogle()).isFalse();
        assertThat(credencial.getCredentialsVersion()).isEqualTo(5L);
        assertThat(vencido.getEstado()).isEqualTo(EstadoCierreCuenta.PURGADA);
        assertThat(vencido.getPurgadaAt()).isNotNull();
        verify(sessionSecurityService).invalidateSessionsForCompany(usuario, company);
        verify(passwordResetTokenRepository).deleteByUsuarioAndCompany(usuario, company);
        verify(auditLogService).log(eq("sistema"), eq("SYSTEM"), eq(7), eq("Vargas Vet"), eq("PURGAR_CREDENCIALES_CIERRE"),
                eq("Seguridad"), anyString(), any());
        InOrder orden = inOrder(credencialRepository, cierreCuentaRepository);
        orden.verify(credencialRepository).save(credencial);
        orden.verify(cierreCuentaRepository).save(vencido);
    }

    @Test
    void sinCierresVencidosNoSeHaceNada() {
        when(cierreCuentaRepository.findTop100ByEstadoAndVenceAtBeforeOrderByVenceAtAsc(eq(EstadoCierreCuenta.CERRADA), any()))
                .thenReturn(List.of());

        assertThat(service.purgeExpired()).isZero();

        verifyNoInteractions(credencialRepository, auditLogService);
    }
}
