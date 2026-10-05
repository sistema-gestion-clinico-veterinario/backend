package veterinaria.vargasvet.ers.multiempresa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.DiaSemana;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.dto.request.ChangePasswordDTO;
import veterinaria.vargasvet.dto.request.HorarioEmpleadoRequest;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyExceptionRepository;
import veterinaria.vargasvet.repository.CompanyOperatingHourRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.repository.HorarioEmpleadoRepository;
import veterinaria.vargasvet.repository.MovimientoCajaRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.PasswordPolicyService;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.ApoderadoServiceImpl;
import veterinaria.vargasvet.service.impl.CajaServiceImpl;
import veterinaria.vargasvet.service.impl.CompanyMembershipServiceImpl;
import veterinaria.vargasvet.service.impl.EmpleadoServiceImpl;
import veterinaria.vargasvet.service.impl.UsuarioServiceImpl;
import veterinaria.vargasvet.util.BusinessValidator;

import java.math.BigDecimal;
import veterinaria.vargasvet.dto.request.AperturaCajaRequest;
import veterinaria.vargasvet.dto.request.ArqueoCajaRequest;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.PuntoCobroResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los 7 escenarios mínimos obligatorios del planteamiento multiempresa:
 * identidad única con relaciones independientes por empresa, credencial por empresa,
 * y aislamiento de datos entre empresas. Los escenarios 2 y 5 se adaptan a la decisión
 * final ya confirmada (contraseña independiente por empresa, no una sola contraseña
 * global) - el resto sigue el planteamiento original literalmente.
 */
@DataJpaTest
class MultiEmpresaIsolationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UsuarioPorRolRepository usuarioPorRolRepository;
    @Autowired private veterinaria.vargasvet.repository.CierreCuentaRepository cierreCuentaRepository;
    @Autowired private veterinaria.vargasvet.repository.CajaRepository cajaRepository;
    @Autowired private veterinaria.vargasvet.repository.CodigoVerificacionRepository codigoVerificacionRepository;
    @Autowired private UsuarioEmpresaCredencialRepository credencialRepository;
    @Autowired private veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository contactoRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private UsuarioMembresiaRepository usuarioMembresiaRepository;
    @Autowired private HorarioEmpleadoRepository horarioEmpleadoRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MovimientoCajaRepository movimientoCajaRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private SesionCajaRepository sesionCajaRepository;
    @Autowired private CompanyOperatingHourRepository companyOperatingHourRepository;
    @Autowired private CompanyExceptionRepository companyExceptionRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TipoEmpleadoRepository tipoEmpleadoRepository;
    @Autowired private veterinaria.vargasvet.repository.MascotaRepository mascotaRepository;
    @Autowired private veterinaria.vargasvet.repository.RazaRepository razaRepository;
    @Autowired private veterinaria.vargasvet.repository.ServiciosVeterinariosRepository serviciosVeterinariosRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioServiceImpl usuarioService;
    private EmpleadoServiceImpl empleadoService;
    private ApoderadoServiceImpl apoderadoService;
    private CajaServiceImpl cajaService;

    @BeforeEach
    void setUp() {
        veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard =
                new veterinaria.vargasvet.service.AccountClosureGuard(cierreCuentaRepository);
        CompanyMembershipService companyMembershipService = new CompanyMembershipServiceImpl(
                empleadoRepository, apoderadoRepository, usuarioMembresiaRepository, usuarioRepository, companyRepository);
        SessionSecurityService sessionSecurityService = new SessionSecurityService(
                usuarioRepository, refreshTokenRepository, credencialRepository,
                mock(veterinaria.vargasvet.security.RealtimeSubscriptionGuard.class));
        UserMapper userMapper = new UserMapper(new ModelMapper());
        TokenProvider tokenProvider = mock(TokenProvider.class);
        java.util.concurrent.atomic.AtomicInteger tokenCounter = new java.util.concurrent.atomic.AtomicInteger();
        when(tokenProvider.createRefreshToken(any(), any(), any(), any(), anyLong()))
                .thenAnswer(invocation -> "refresh-token-fake-" + tokenCounter.incrementAndGet());
        when(tokenProvider.getRefreshTokenDetails(any())).thenAnswer(invocation ->
                new TokenProvider.RefreshTokenDetails("test@example.test", "jti-fake", "family-fake", null, null, 0L));
        MenuBuilderService menuBuilderService = mock(MenuBuilderService.class);
        when(menuBuilderService.construirMenuJerarquico(any(), any())).thenReturn(List.of());
        when(menuBuilderService.construirPermissions(any(), any())).thenReturn(List.of());

        veterinaria.vargasvet.service.impl.UsuarioContactoService contactoService =
                new veterinaria.vargasvet.service.impl.UsuarioContactoService(contactoRepository, companyRepository);

        usuarioService = new UsuarioServiceImpl(
                usuarioRepository, empleadoRepository, apoderadoRepository, credencialRepository,
                roleRepository, passwordEncoder, userMapper, tokenProvider, mock(EmailService.class),
                menuBuilderService, refreshTokenRepository, usuarioPorRolRepository, passwordResetTokenRepository,
                mock(AuditLogService.class), companyRepository, companyMembershipService, sessionSecurityService,
                mock(SharedRateLimitService.class), mock(veterinaria.vargasvet.security.AccountLockoutService.class),
                mock(AuthenticationAuditService.class),
                new PasswordPolicyService(), mock(LegalDocumentService.class), contactoService, mock(veterinaria.vargasvet.security.RealtimeSubscriptionGuard.class),
                new veterinaria.vargasvet.service.AdministratorProtection(usuarioPorRolRepository, companyMembershipService, companyRepository),
                accountClosureGuard,
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));

        empleadoService = new EmpleadoServiceImpl(
                usuarioRepository, roleRepository, empleadoRepository, especialidadRepository, tipoEmpleadoRepository,
                companyRepository, horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository,
                citaRepository, passwordEncoder, userMapper, mock(EmailService.class), mock(BusinessValidator.class),
                mock(AuditLogService.class), usuarioPorRolRepository, sessionSecurityService, companyMembershipService,
                credencialRepository, contactoService,
                new veterinaria.vargasvet.service.AdministratorProtection(usuarioPorRolRepository, companyMembershipService, companyRepository),
                accountClosureGuard,
                new veterinaria.vargasvet.service.CajasAbiertasDelPersonal(sesionCajaRepository, cajaRepository),
                mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));

        apoderadoService = new ApoderadoServiceImpl(
                usuarioRepository, apoderadoRepository, mascotaRepository,
                refreshTokenRepository, usuarioPorRolRepository, roleRepository, companyRepository, passwordEncoder,
                userMapper, mock(BusinessValidator.class), mock(EmailService.class), mock(AuditLogService.class),
                mock(veterinaria.vargasvet.service.CompanyRoleProvisioningService.class), sessionSecurityService,
                companyMembershipService, citaRepository, credencialRepository, contactoService,
                petOwnership(),
                accountClosureGuard,
                new veterinaria.vargasvet.service.AdministratorProtection(usuarioPorRolRepository, companyMembershipService, companyRepository),
                mock(veterinaria.vargasvet.service.AccessRestoredNotifier.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.service.ConsentimientoDatosService.class));

        org.springframework.test.util.ReflectionTestUtils.setField(empleadoService, "entityManager", entityManager);
        org.springframework.test.util.ReflectionTestUtils.setField(apoderadoService, "entityManager", entityManager);

        cajaService = new CajaServiceImpl(
                movimientoCajaRepository, citaRepository, purchaseRepository, sesionCajaRepository,
                mock(AuditLogService.class), usuarioRepository,
                new veterinaria.vargasvet.service.PuntoCobroService(cajaRepository, sesionCajaRepository, usuarioRepository,
                        new veterinaria.vargasvet.security.CajaDispositivoCookie(), mock(AuditLogService.class), companyRepository),
                cajaRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Helpers de fixtures ----------

    private Company crearEmpresa(String nombre, String slug) {
        Company company = new Company();
        company.setName(nombre);
        company.setSlug(slug);
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Role crearRol(Company company, String nombre) {
        Role role = new Role();
        role.setName(nombre);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(RolePurpose.CUSTOM);
        role.setActivo(true);
        role.setCompany(company);
        return roleRepository.save(role);
    }

    private Role crearRolCliente(Company company) {
        Role role = new Role();
        role.setName("ROLE_CLIENTE");
        role.setScope(RoleScope.CLIENT);
        role.setPurpose(RolePurpose.CLIENT_PORTAL);
        role.setActivo(true);
        role.setCompany(company);
        return roleRepository.save(role);
    }

    private Usuario crearIdentidad(String email, String username, String dni) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setUsername(username);
        usuario.setDni(dni);
        usuario.setNombre("Sebastián");
        usuario.setApellido("Prueba");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.save(usuario);
    }

    private void crearCredencial(Usuario usuario, Company company, String rawPassword) {
        UsuarioEmpresaCredencial c = new UsuarioEmpresaCredencial();
        c.setUsuario(usuario);
        c.setCompany(company);
        c.setPassword(passwordEncoder.encode(rawPassword));
        c.setPasswordChanged(true);
        c.setCreatedAt(LocalDateTime.now());
        credencialRepository.save(c);
    }

    private Empleado crearEmpleado(Usuario usuario, Company company) {
        Empleado e = new Empleado();
        e.setUser(usuario);
        e.setCompany(company);
        e.setEstado(true);
        e.setGenero(Genero.MASCULINO);
        e.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        e.setNumeroDocumentoIdentidad(usuario.getDni());
        e.setFechaIngreso(LocalDate.now());
        return empleadoRepository.save(e);
    }

    private Apoderado crearApoderado(Usuario usuario, Company company) {
        Apoderado a = new Apoderado();
        a.setUser(usuario);
        a.setCompany(company);
        a.setEstado(true);
        a.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        a.setNumeroDocumento(usuario.getDni());
        a.setGenero(Genero.MASCULINO);
        return apoderadoRepository.save(a);
    }

    private void asignarRol(Usuario usuario, Role role, Company company) {
        UsuarioPorRol upr = new UsuarioPorRol();
        upr.setUsuario(usuario);
        upr.setRol(role);
        upr.setCompany(company);
        usuarioPorRolRepository.save(upr);
        // usuario.usuariosPorRol es una lista Java normal (no un PersistentBag) hasta que
        // Hibernate recarga la entidad desde la BD - como el test reutiliza la MISMA
        // instancia manejada dentro de la transacción, hay que reflejar el cambio a mano
        // para que login() (que reconsulta por username dentro de la misma sesión) lo vea.
        usuario.getUsuariosPorRol().add(upr);
    }

    private LoginDTO login(String username, String password, String slug) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setSlug(slug);
        return dto;
    }

    private void autenticarComo(Integer usuarioId, Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                usuarioId, "test@example.test", "n/a", List.of(new SimpleGrantedAuthority("ROLE_TEST")), companyId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    // ---------- Escenario 1 ----------

    @Test
    @DisplayName("[Escenario 1] Empleado de una sola empresa se autentica normalmente")
    void escenario1_empleadoDeUnaSolaEmpresaIniciaSesionNormalmente() {
        Company vargasVet = crearEmpresa("Vargas Vet", "vargas-vet");
        Role rolVeterinario = crearRol(vargasVet, "ROLE_VETERINARIO");
        Usuario sebastian = crearIdentidad("sebastian@gmail.com", "sebastian.vv", "11111111");
        crearCredencial(sebastian, vargasVet, "Vargas123!");
        crearEmpleado(sebastian, vargasVet);
        asignarRol(sebastian, rolVeterinario, vargasVet);

        AuthResponse response = usuarioService.login(login("sebastian.vv", "Vargas123!", "vargas-vet"));

        assertThat(response.getCompanyId()).isEqualTo(vargasVet.getId());
        assertThat(response.getActiveRoleName()).isEqualTo("ROLE_VETERINARIO");
    }

    // ---------- Escenario 2 (adaptado a credencial por empresa) ----------

    @Test
    @DisplayName("[Escenario 2] Misma persona en dos empresas con contraseñas independientes puede cambiar de contexto")
    void escenario2_mismaPersonaEnDosEmpresasConCredencialesIndependientes() {
        Company vargasVet = crearEmpresa("Vargas Vet", "vargas-vet-2");
        Company duke = crearEmpresa("El Duke de Can", "el-duke-de-can-2");
        Role rolClienteVargas = crearRolCliente(vargasVet);
        Role rolClienteDuke = crearRolCliente(duke);

        Usuario sebastian = crearIdentidad("sebastian2@gmail.com", "sebastian.dos", "22222222");
        crearCredencial(sebastian, vargasVet, "Vargas123!");
        crearCredencial(sebastian, duke, "Duke456!");
        crearApoderado(sebastian, vargasVet);
        crearApoderado(sebastian, duke);
        asignarRol(sebastian, rolClienteVargas, vargasVet);
        asignarRol(sebastian, rolClienteDuke, duke);

        AuthResponse enVargas = usuarioService.login(login("sebastian.dos", "Vargas123!", "vargas-vet-2"));
        assertThat(enVargas.getCompanyId()).isEqualTo(vargasVet.getId());

        AuthResponse enDuke = usuarioService.login(login("sebastian.dos", "Duke456!", "el-duke-de-can-2"));
        assertThat(enDuke.getCompanyId()).isEqualTo(duke.getId());

        // La contraseña de una empresa NUNCA sirve para la otra.
        assertThatThrownBy(() -> usuarioService.login(login("sebastian.dos", "Duke456!", "vargas-vet-2")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> usuarioService.login(login("sebastian.dos", "Vargas123!", "el-duke-de-can-2")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ---------- Escenario 3 ----------

    @Test
    @DisplayName("[Escenario 3] Sebastián: veterinario en Vargas Vet, apoderado en El Duke de Can, roles distintos por contexto")
    void escenario3_empleadoEnUnaEmpresaYApoderadoEnOtraObtieneRolesDistintos() {
        Company vargasVet = crearEmpresa("Vargas Vet", "vargas-vet-3");
        Company duke = crearEmpresa("El Duke de Can", "el-duke-de-can-3");
        Role rolVeterinario = crearRol(vargasVet, "ROLE_VETERINARIO");
        Role rolCliente = crearRolCliente(duke);

        Usuario sebastian = crearIdentidad("sebastian3@gmail.com", "sebastian.tres", "33333333");
        crearCredencial(sebastian, vargasVet, "Vargas123!");
        crearCredencial(sebastian, duke, "Duke456!");
        crearEmpleado(sebastian, vargasVet);
        crearApoderado(sebastian, duke);
        asignarRol(sebastian, rolVeterinario, vargasVet);
        asignarRol(sebastian, rolCliente, duke);

        AuthResponse comoVeterinario = usuarioService.login(login("sebastian.tres", "Vargas123!", "vargas-vet-3"));
        assertThat(comoVeterinario.getActiveRoleName()).isEqualTo("ROLE_VETERINARIO");

        AuthResponse comoCliente = usuarioService.login(login("sebastian.tres", "Duke456!", "el-duke-de-can-3"));
        assertThat(comoCliente.getActiveRoleName()).isEqualTo("ROLE_CLIENTE");

        // Nunca "veterinario en alguna empresa, por tanto veterinario en todas".
        assertThat(comoCliente.getActiveRoleName()).isNotEqualTo("ROLE_VETERINARIO");
    }

    // ---------- Escenario 4 ----------

    @Test
    @DisplayName("[Escenario 4] Acceso cruzado por ID: no se puede registrar una devolución de caja de otra empresa")
    void escenario4_accesoCruzadoPorIdEsRechazado() {
        Company empresaA = crearEmpresa("Empresa A", "empresa-a-4");
        Company empresaB = crearEmpresa("Empresa B", "empresa-b-4");
        Cita citaDeB = crearCitaCanceladaConPago(empresaB);

        autenticarComo(1, empresaA.getId());

        assertThatThrownBy(() -> cajaService.registrarDevolucion(citaDeB.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra sede");
    }

    // ---------- Escenario 5 (adaptado a credencial por empresa) ----------

    @Test
    @DisplayName("[Escenario 5] Cambiar la contraseña en una empresa no afecta la contraseña en la otra")
    void escenario5_cambiarContrasenaSoloAfectaLaEmpresaActiva() {
        Company vargasVet = crearEmpresa("Vargas Vet", "vargas-vet-5");
        Company duke = crearEmpresa("El Duke de Can", "el-duke-de-can-5");
        Usuario sebastian = crearIdentidad("sebastian5@gmail.com", "sebastian.cinco", "55555555");
        crearCredencial(sebastian, vargasVet, "Vargas123!");
        crearCredencial(sebastian, duke, "Duke456!");
        crearApoderado(sebastian, vargasVet);
        crearApoderado(sebastian, duke);
        asignarRol(sebastian, crearRolCliente(vargasVet), vargasVet);
        asignarRol(sebastian, crearRolCliente(duke), duke);

        autenticarComo(sebastian.getId(), vargasVet.getId());
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("Vargas123!");
        dto.setNewPassword("NuevaClaveVV2026!");
        usuarioService.changePassword(sebastian.getId(), dto);

        // La nueva funciona, la vieja de Vargas Vet deja de servir...
        AuthResponse conNueva = usuarioService.login(login("sebastian.cinco", "NuevaClaveVV2026!", "vargas-vet-5"));
        assertThat(conNueva.getCompanyId()).isEqualTo(vargasVet.getId());
        assertThatThrownBy(() -> usuarioService.login(login("sebastian.cinco", "Vargas123!", "vargas-vet-5")))
                .isInstanceOf(BadCredentialsException.class);

        // ...pero la de El Duke de Can sigue intacta, nunca se tocó.
        AuthResponse enDuke = usuarioService.login(login("sebastian.cinco", "Duke456!", "el-duke-de-can-5"));
        assertThat(enDuke.getCompanyId()).isEqualTo(duke.getId());
    }

    // ---------- Escenario 6 ----------

    @Test
    @DisplayName("[Escenario 6] Modificar el horario de un empleado de la Empresa A nunca toca el de la Empresa B")
    void escenario6_modificarDatosDeUnaEmpresaNuncaAfectaALaOtra() {
        Company empresaA = crearEmpresa("Empresa A", "empresa-a-6");
        Company empresaB = crearEmpresa("Empresa B", "empresa-b-6");
        Usuario empleadoUsuarioA = crearIdentidad("empleadoA6@example.test", "empleado.a6", "66666661");
        Usuario empleadoUsuarioB = crearIdentidad("empleadoB6@example.test", "empleado.b6", "66666662");
        Empleado empleadoA = crearEmpleado(empleadoUsuarioA, empresaA);
        Empleado empleadoB = crearEmpleado(empleadoUsuarioB, empresaB);
        HorarioEmpleado horarioA = crearHorario(empleadoA, LocalDate.now().plusDays(10));
        HorarioEmpleado horarioB = crearHorario(empleadoB, LocalDate.now().plusDays(10));

        autenticarComo(1, empresaA.getId());
        HorarioEmpleadoRequest request = new HorarioEmpleadoRequest();
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(14, 0));
        empleadoService.updateHorario(horarioA.getId(), request);

        HorarioEmpleado horarioBSinTocar = horarioEmpleadoRepository.findById(horarioB.getId()).orElseThrow();
        assertThat(horarioBSinTocar.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(horarioBSinTocar.getHoraFin()).isEqualTo(LocalTime.of(12, 0));

        HorarioEmpleado horarioAActualizado = horarioEmpleadoRepository.findById(horarioA.getId()).orElseThrow();
        assertThat(horarioAActualizado.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
    }

    // ---------- Escenario 7 ----------

    @Test
    @DisplayName("[Escenario 7] IDs de recursos de dos empresas nunca se cruzan al editar/eliminar horarios")
    void escenario7_idsParecidosEntreEmpresasNuncaSeCruzan() {
        Company empresaA = crearEmpresa("Empresa A", "empresa-a-7");
        Company empresaB = crearEmpresa("Empresa B", "empresa-b-7");
        Usuario empleadoUsuarioA = crearIdentidad("empleadoA7@example.test", "empleado.a7", "77777771");
        Usuario empleadoUsuarioB = crearIdentidad("empleadoB7@example.test", "empleado.b7", "77777772");
        Empleado empleadoA = crearEmpleado(empleadoUsuarioA, empresaA);
        Empleado empleadoB = crearEmpleado(empleadoUsuarioB, empresaB);
        HorarioEmpleado horarioB = crearHorario(empleadoB, LocalDate.now().plusDays(11));

        autenticarComo(1, empresaA.getId());

        // Empresa A intenta editar y eliminar un horario cuyo ID pertenece a la Empresa B.
        HorarioEmpleadoRequest request = new HorarioEmpleadoRequest();
        request.setHoraInicio(LocalTime.of(9, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        assertThatThrownBy(() -> empleadoService.updateHorario(horarioB.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra empresa");
        assertThatThrownBy(() -> empleadoService.deleteHorario(horarioB.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otra empresa");

        // El horario de la Empresa B sigue existiendo, sin tocar.
        assertThat(horarioEmpleadoRepository.findById(horarioB.getId())).isPresent();
    }

    // ---------- Escenario 8 ----------

    private Usuario crearIdentidadDeEmpresa(String email, String username, String dni, Company company) {
        Usuario usuario = crearIdentidad(email, username, dni);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private veterinaria.vargasvet.service.PetOwnershipService petOwnership() {
        veterinaria.vargasvet.service.PetOwnershipService service = new veterinaria.vargasvet.service.PetOwnershipService(
                mascotaRepository, mock(veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository.class),
                citaRepository, mock(AuditLogService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);
        return service;
    }

    private void autenticarComoAdministrador(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "admin@example.test", "n/a", List.of(), companyId,
                2, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private veterinaria.vargasvet.dto.request.AdminPasswordResetRequest restablecer(Integer userId) {
        veterinaria.vargasvet.dto.request.AdminPasswordResetRequest request =
                new veterinaria.vargasvet.dto.request.AdminPasswordResetRequest();
        request.setUserId(userId);
        return request;
    }

    @Test
    @DisplayName("[Escenario 8] Restablecimiento administrativo: la misma persona en dos clínicas es una cuenta por clínica y solo se alcanza la propia")
    void escenario8_restablecimientoAdministrativoSoloAlcanzaLaCuentaDeLaClinicaDelAdministrador() {
        Company clinicaA = crearEmpresa("Clinica A", "clinica-a-8");
        Company clinicaB = crearEmpresa("Clinica B", "clinica-b-8");
        Usuario anaEnA = crearIdentidadDeEmpresa("ana8@example.test", "ana.a8", "88888881", clinicaA);
        Usuario anaEnB = crearIdentidadDeEmpresa("ana8@example.test", "ana.b8", "88888882", clinicaB);
        crearCredencial(anaEnA, clinicaA, "ClaveA123!");
        crearCredencial(anaEnB, clinicaB, "ClaveB123!");
        crearEmpleado(anaEnA, clinicaA);
        crearEmpleado(anaEnB, clinicaB);
        autenticarComoAdministrador(clinicaA.getId());

        usuarioService.requestPasswordReset(restablecer(anaEnA.getId()));

        List<veterinaria.vargasvet.domain.entity.PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getUsuario().getId()).isEqualTo(anaEnA.getId());
        assertThat(tokens.get(0).getCompany().getId()).isEqualTo(clinicaA.getId());

        assertThatThrownBy(() -> usuarioService.requestPasswordReset(restablecer(anaEnB.getId())))
                .isInstanceOf(veterinaria.vargasvet.exception.ResourceNotFoundException.class);
        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
    }

    // ---------- Escenario 9 ----------

    @Test
    @DisplayName("[Escenario 9] Suspendida en una clínica y activa en otra: con Google la suspensión de A nunca bloquea a B")
    void escenario9_laSuspensionDeUnaClinicaNoAfectaAOtraConGoogle() {
        Company clinicaA = crearEmpresa("Clinica A", "clinica-a-9");
        Company clinicaB = crearEmpresa("Clinica B", "clinica-b-9");
        Role rolA = crearRol(clinicaA, "ROLE_VETERINARIO");
        Role rolB = crearRolCliente(clinicaB);
        Usuario ana = crearIdentidad("ana9@example.test", "ana.nueve", "99999991");
        crearCredencial(ana, clinicaA, "ClaveA123!");
        crearCredencial(ana, clinicaB, "ClaveB123!");
        Empleado empleadoEnA = crearEmpleado(ana, clinicaA);
        crearApoderado(ana, clinicaB);
        asignarRol(ana, rolA, clinicaA);
        asignarRol(ana, rolB, clinicaB);
        empleadoEnA.setEstado(false);
        empleadoEnA.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);
        empleadoRepository.save(empleadoEnA);

        assertThatThrownBy(() -> usuarioService.loginWithGoogle("ana9@example.test", "clinica-a-9"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountSuspendedException.class);
        AuthResponse enB = usuarioService.loginWithGoogle("ana9@example.test", "clinica-b-9");
        assertThat(enB.getCompanyId()).isEqualTo(clinicaB.getId());

        assertThat(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(
                ana.getId(), clinicaA.getId(), veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION)).isTrue();
        assertThat(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(
                ana.getId(), clinicaB.getId(), veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION)).isFalse();

        empleadoEnA.setTipoInactividad(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        empleadoRepository.save(empleadoEnA);
        assertThatThrownBy(() -> usuarioService.loginWithGoogle("ana9@example.test", "clinica-a-9"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountDeactivatedException.class);
        assertThat(usuarioService.loginWithGoogle("ana9@example.test", "clinica-b-9").getCompanyId())
                .isEqualTo(clinicaB.getId());
    }

    // ---------- Escenario 10 ----------

    @Test
    @DisplayName("[Escenario 10] Editar o dar de baja a una persona desde una clínica nunca toca su acceso ni sus roles en otra")
    void escenario10_laGestionDeUnaClinicaNoAfectaALaOtra() {
        Company clinicaA = crearEmpresa("Clinica A", "clinica-a-10");
        Company clinicaB = crearEmpresa("Clinica B", "clinica-b-10");
        Role veterinario = crearRol(clinicaA, "ROLE_VETERINARIO");
        Role recepcion = crearRol(clinicaA, "ROLE_RECEPCION");
        Role cliente = crearRolCliente(clinicaB);
        Usuario ana = crearIdentidad("ana10@example.test", "ana.diez", "10101010");
        crearCredencial(ana, clinicaA, "ClaveA123!");
        crearCredencial(ana, clinicaB, "ClaveB123!");
        Empleado empleado = crearEmpleado(ana, clinicaA);
        crearApoderado(ana, clinicaB);
        asignarRol(ana, veterinario, clinicaA);
        asignarRol(ana, cliente, clinicaB);
        new CompanyMembershipServiceImpl(empleadoRepository, apoderadoRepository, usuarioMembresiaRepository,
                usuarioRepository, companyRepository).syncLegacyCompanyField(ana);
        usuarioRepository.flush();
        assertThat(ana.getCompany()).isNull();
        autenticarComoAdministrador(clinicaA.getId());

        veterinaria.vargasvet.dto.request.EmpleadoRequest cambio = new veterinaria.vargasvet.dto.request.EmpleadoRequest();
        cambio.setRoleIds(java.util.Set.of(recepcion.getId()));
        empleadoService.updateEmpleado(empleado.getId(), cambio);

        assertThat(usuarioPorRolRepository.findByUsuarioId(ana.getId()))
                .extracting(asignacion -> asignacion.getRol().getName())
                .containsExactlyInAnyOrder("ROLE_RECEPCION", "ROLE_CLIENTE");

        empleadoService.cambiarEstado(empleado.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Fin de contrato");

        assertThat(ana.isActivo()).isTrue();
        assertThat(usuarioService.loginWithGoogle("ana10@example.test", "clinica-b-10").getCompanyId())
                .isEqualTo(clinicaB.getId());
        assertThatThrownBy(() -> usuarioService.loginWithGoogle("ana10@example.test", "clinica-a-10"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountDeactivatedException.class);

        empleadoService.cambiarEstado(empleado.getId(), true, null, null);

        assertThat(usuarioService.loginWithGoogle("ana10@example.test", "clinica-a-10").getCompanyId())
                .isEqualTo(clinicaA.getId());
    }

    // ---------- Escenario 11 ----------

    private void autenticarComoCliente(Integer usuarioId, Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuarioId, "ana11@example.test", "n/a", List.of(), companyId,
                2, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("[Escenario 11] Cerrar la cuenta en una clínica corta el acceso solo ahí, se reactiva con el enlace y la purga borra solo esa credencial")
    void escenario11_cerrarLaCuentaEnUnaClinicaNoTocaALaOtra() {
        Company clinicaA = crearEmpresa("Clinica A", "clinica-a-11");
        Company clinicaB = crearEmpresa("Clinica B", "clinica-b-11");
        Role clienteA = crearRolCliente(clinicaA);
        Role clienteB = crearRolCliente(clinicaB);
        Usuario ana = crearIdentidad("ana11@example.test", "ana.once", "11111111");
        crearCredencial(ana, clinicaA, "ClaveA123!");
        crearCredencial(ana, clinicaB, "ClaveB123!");
        Apoderado enA = crearApoderado(ana, clinicaA);
        Apoderado enB = crearApoderado(ana, clinicaB);
        asignarRol(ana, clienteA, clinicaA);
        asignarRol(ana, clienteB, clinicaB);
        new CompanyMembershipServiceImpl(empleadoRepository, apoderadoRepository, usuarioMembresiaRepository,
                usuarioRepository, companyRepository).syncLegacyCompanyField(ana);
        usuarioRepository.flush();

        EmailService emailService = mock(EmailService.class);
        when(emailService.createMail(any(), any(), any())).thenReturn(new veterinaria.vargasvet.dto.Mail());
        veterinaria.vargasvet.service.VerificationCodeService codigos =
                new veterinaria.vargasvet.service.VerificationCodeService(codigoVerificacionRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(codigos, "validityMinutes", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(codigos, "maxAttempts", 5);
        CompanyMembershipService membresias = new CompanyMembershipServiceImpl(empleadoRepository, apoderadoRepository,
                usuarioMembresiaRepository, usuarioRepository, companyRepository);
        veterinaria.vargasvet.service.AccountClosureService cierre = new veterinaria.vargasvet.service.AccountClosureService(
                usuarioRepository, companyRepository, empleadoRepository, apoderadoRepository, credencialRepository,
                cierreCuentaRepository, sesionCajaRepository, citaRepository, passwordResetTokenRepository, passwordEncoder,
                new veterinaria.vargasvet.service.AdministratorProtection(usuarioPorRolRepository, membresias, companyRepository),
                petOwnership(),
                new SessionSecurityService(usuarioRepository, refreshTokenRepository, credencialRepository,
                        mock(veterinaria.vargasvet.security.RealtimeSubscriptionGuard.class)),
                membresias, codigos, mock(SharedRateLimitService.class), emailService, mock(AuditLogService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(cierre, "graceDays", 30L);
        org.springframework.test.util.ReflectionTestUtils.setField(cierre, "codeValidityMinutes", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(cierre, "frontendUrl", "https://app.test");
        org.springframework.test.util.ReflectionTestUtils.setField(cierre, "defaultCompanyName", "SoftVet");

        // Cierre en la clínica A
        autenticarComoCliente(ana.getId(), clinicaA.getId());
        assertThat(cierre.eligibility().eligible()).isTrue();
        cierre.requestClosure("ClaveA123!");
        String codigo = valorDelUltimoCorreo(emailService, "code");
        cierre.confirmClosure(codigo);
        String token = valorDelUltimoCorreo(emailService, "reactivateUrl").split("#token=")[1];

        assertThat(enA.getEstado()).isFalse();
        assertThat(enA.getTipoInactividad()).isEqualTo(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        assertThat(enB.getEstado()).isTrue();
        assertThat(cierreCuentaRepository.existsByUsuarioIdAndCompanyIdAndEstado(ana.getId(), clinicaA.getId(),
                veterinaria.vargasvet.domain.enums.EstadoCierreCuenta.CERRADA)).isTrue();
        assertThat(cierreCuentaRepository.existsByUsuarioIdAndCompanyIdAndEstado(ana.getId(), clinicaB.getId(),
                veterinaria.vargasvet.domain.enums.EstadoCierreCuenta.CERRADA)).isFalse();
        assertThat(usuarioService.loginWithGoogle("ana11@example.test", "clinica-b-11").getCompanyId())
                .isEqualTo(clinicaB.getId());
        assertThat(usuarioService.login(login("ana.once", "ClaveB123!", "clinica-b-11")).getCompanyId())
                .isEqualTo(clinicaB.getId());
        assertThatThrownBy(() -> usuarioService.loginWithGoogle("ana11@example.test", "clinica-a-11"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountClosedException.class);
        assertThatThrownBy(() -> usuarioService.login(login("ana.once", "ClaveA123!", "clinica-a-11")))
                .isInstanceOf(BadCredentialsException.class);

        // El administrador de A no puede reactivar lo que la persona cerró
        autenticarComoAdministrador(clinicaA.getId());
        assertThatThrownBy(() -> apoderadoService.cambiarEstado(enA.getId(), true, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cerró su propia cuenta");
        assertThat(enA.getEstado()).isFalse();

        // La persona la reactiva con el enlace de su correo, una sola vez
        SecurityContextHolder.clearContext();
        cierre.reactivate(token);
        assertThat(enA.getEstado()).isTrue();
        assertThat(usuarioService.loginWithGoogle("ana11@example.test", "clinica-a-11").getCompanyId())
                .isEqualTo(clinicaA.getId());
        assertThatThrownBy(() -> cierre.reactivate(token))
                .isInstanceOf(veterinaria.vargasvet.exception.ResourceNotFoundException.class);

        // Cierra de nuevo y vence el plazo: se borra el secreto de A y solo el de A
        autenticarComoCliente(ana.getId(), clinicaA.getId());
        cierre.requestClosure("ClaveA123!");
        cierre.confirmClosure(valorDelUltimoCorreo(emailService, "code"));
        veterinaria.vargasvet.domain.entity.CierreCuenta abierto = cierreCuentaRepository.findAll().stream()
                .filter(c -> c.getEstado() == veterinaria.vargasvet.domain.enums.EstadoCierreCuenta.CERRADA)
                .findFirst().orElseThrow();
        abierto.setVenceAt(veterinaria.vargasvet.util.AppClock.now().minusDays(1));
        cierreCuentaRepository.saveAndFlush(abierto);
        SecurityContextHolder.clearContext();

        assertThat(cierre.purgeExpired()).isEqualTo(1);

        assertThat(abierto.getEstado()).isEqualTo(veterinaria.vargasvet.domain.enums.EstadoCierreCuenta.PURGADA);
        assertThat(credencialRepository.findByUsuarioIdAndCompanyId(ana.getId(), clinicaA.getId()).orElseThrow().isPasswordChanged())
                .isFalse();
        assertThat(credencialRepository.findByUsuarioIdAndCompanyId(ana.getId(), clinicaB.getId()).orElseThrow().isPasswordChanged())
                .isTrue();
        assertThat(usuarioService.login(login("ana.once", "ClaveB123!", "clinica-b-11")).getCompanyId())
                .isEqualTo(clinicaB.getId());
        autenticarComoAdministrador(clinicaA.getId());
        apoderadoService.cambiarEstado(enA.getId(), true, null, null);
        assertThat(enA.getEstado()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private String valorDelUltimoCorreo(EmailService emailService, String clave) {
        org.mockito.ArgumentCaptor<java.util.Map<String, Object>> modelo = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        org.mockito.Mockito.verify(emailService, org.mockito.Mockito.atLeastOnce()).createMail(any(), any(), modelo.capture());
        return modelo.getAllValues().stream()
                .filter(m -> m.containsKey(clave))
                .reduce((primero, ultimo) -> ultimo)
                .map(m -> String.valueOf(m.get(clave)))
                .orElseThrow();
    }

    // ---------- Escenario 12 ----------

    private void comoPersonaConEquipo(Usuario persona, Company empresa, RolePurpose purpose, String token) {
        UsuarioPrincipal principal = new UsuarioPrincipal(persona.getId(), persona.getEmail(), "n/a", List.of(),
                empresa.getId(), 2, RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, principal.getAuthorities()));
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        if (token != null) {
            request.setCookies(new jakarta.servlet.http.Cookie(
                    veterinaria.vargasvet.security.CajaDispositivoCookie.nombre(empresa.getSlug()), token));
        }
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
    }

    private String tokenDe(org.springframework.mock.web.MockHttpServletResponse respuesta) {
        String cabecera = respuesta.getHeader("Set-Cookie");
        return cabecera.substring(cabecera.indexOf('=') + 1, cabecera.indexOf(';'));
    }

    @Test
    @DisplayName("[Escenario 12] Dos equipos de un mostrador abren su propia caja a la vez, con totales separados y sin cruzar sedes")
    void escenario12_variasCajasAbiertasAlMismoTiempo() {
        Company sedeA = crearEmpresa("Sede A", "sede-a-12");
        Company sedeB = crearEmpresa("Sede B", "sede-b-12");
        Usuario admin = crearIdentidadDeEmpresa("admin12@example.test", "admin.doce", "12121210", sedeA);
        Usuario ana = crearIdentidadDeEmpresa("ana12@example.test", "ana.doce", "12121211", sedeA);
        Usuario luis = crearIdentidadDeEmpresa("luis12@example.test", "luis.doce", "12121212", sedeA);
        veterinaria.vargasvet.service.PuntoCobroService puntos = new veterinaria.vargasvet.service.PuntoCobroService(
                cajaRepository, sesionCajaRepository, usuarioRepository,
                new veterinaria.vargasvet.security.CajaDispositivoCookie(), mock(AuditLogService.class), companyRepository);

        // El administrador crea dos puntos de cobro y registra un equipo para cada uno
        comoPersonaConEquipo(admin, sedeA, RolePurpose.COMPANY_ADMIN, null);
        puntos.listar(sedeA.getId());
        Long uno = puntos.crear(sedeA.getId(), "Mostrador 1").id();
        Long dos = puntos.crear(sedeA.getId(), "Mostrador 2").id();
        org.springframework.mock.web.MockHttpServletResponse respuestaUno = new org.springframework.mock.web.MockHttpServletResponse();
        puntos.vincular(sedeA.getId(), uno, new org.springframework.mock.web.MockHttpServletRequest(), respuestaUno);
        org.springframework.mock.web.MockHttpServletResponse respuestaDos = new org.springframework.mock.web.MockHttpServletResponse();
        puntos.vincular(sedeA.getId(), dos, new org.springframework.mock.web.MockHttpServletRequest(), respuestaDos);
        String equipoUno = tokenDe(respuestaUno);
        String equipoDos = tokenDe(respuestaDos);
        assertThat(equipoUno).isNotEqualTo(equipoDos);

        // Cada persona abre la caja de su equipo, a la vez
        comoPersonaConEquipo(ana, sedeA, RolePurpose.CUSTOM, equipoUno);
        AperturaCajaRequest aperturaUno = new AperturaCajaRequest();
        aperturaUno.setCompanyId(sedeA.getId());
        aperturaUno.setMontoApertura(new BigDecimal("100.00"));
        assertThat(cajaService.abrirCaja(aperturaUno).getCajaNombre()).isEqualTo("Mostrador 1");
        comoPersonaConEquipo(luis, sedeA, RolePurpose.CUSTOM, equipoDos);
        AperturaCajaRequest aperturaDos = new AperturaCajaRequest();
        aperturaDos.setCompanyId(sedeA.getId());
        aperturaDos.setMontoApertura(new BigDecimal("200.00"));
        assertThat(cajaService.abrirCaja(aperturaDos).getCajaNombre()).isEqualTo("Mostrador 2");
        assertThat(sesionCajaRepository.findAllByCompanyIdAndEstado(sedeA.getId(),
                veterinaria.vargasvet.domain.enums.EstadoSesionCaja.ABIERTA)).hasSize(2);

        // Cada equipo registra lo suyo y su total no se mezcla con el del otro
        MovimientoEgresoRequest egresoLuis = new MovimientoEgresoRequest();
        egresoLuis.setCompanyId(sedeA.getId());
        egresoLuis.setMonto(new BigDecimal("50.00"));
        egresoLuis.setDescripcion("Compra de bolsas");
        cajaService.registrarEgreso(egresoLuis);
        assertThat(cajaService.obtenerSesionActual(sedeA.getId()).getEfectivoEsperado()).isEqualByComparingTo("150.00");
        comoPersonaConEquipo(ana, sedeA, RolePurpose.CUSTOM, equipoUno);
        MovimientoEgresoRequest egresoAna = new MovimientoEgresoRequest();
        egresoAna.setCompanyId(sedeA.getId());
        egresoAna.setMonto(new BigDecimal("30.00"));
        egresoAna.setDescripcion("Taxi");
        cajaService.registrarEgreso(egresoAna);
        assertThat(cajaService.obtenerSesionActual(sedeA.getId()).getEfectivoEsperado()).isEqualByComparingTo("70.00");

        // Una persona no puede tener dos cajas abiertas, ni un equipo abrir la caja de otro
        assertThatThrownBy(() -> cajaService.abrirCaja(aperturaUno))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya se encuentra abierta");

        // Un equipo sin registrar, con varias cajas en la sede, no puede cobrar
        comoPersonaConEquipo(ana, sedeA, RolePurpose.CUSTOM, null);
        assertThatThrownBy(() -> cajaService.registrarEgreso(egresoAna))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no está registrado como punto de cobro");
        assertThat(cajaService.obtenerSesionActual(sedeA.getId())).isNull();

        // El equipo de una sede no opera en otra: la otra sede usa su propia caja principal
        comoPersonaConEquipo(admin, sedeB, RolePurpose.COMPANY_ADMIN, equipoUno);
        assertThat(puntos.resolver(sedeB.getId()).caja().getCompanyId()).isEqualTo(sedeB.getId());
        assertThat(puntos.resolver(sedeB.getId()).modo()).isEqualTo("SENCILLO");
        assertThat(puntos.listar(sedeB.getId())).extracting(PuntoCobroResponse::nombre).containsExactly("Caja principal");

        // Un administrador cierra la caja que dejó abierta otra persona; la demás sigue abierta
        comoPersonaConEquipo(admin, sedeA, RolePurpose.COMPANY_ADMIN, null);
        Long sesionDeLuis = sesionCajaRepository.findAllByCompanyIdAndEstado(sedeA.getId(),
                veterinaria.vargasvet.domain.enums.EstadoSesionCaja.ABIERTA).stream()
                .filter(sesion -> luis.getId().equals(sesion.getAbiertaPorUsuarioId())).findFirst().orElseThrow().getId();
        ArqueoCajaRequest cierre = new ArqueoCajaRequest();
        cierre.setCompanyId(sedeA.getId());
        cierre.setEfectivoContado(new BigDecimal("150.00"));
        cierre.setSesionId(sesionDeLuis);
        assertThat(cajaService.cerrarCaja(cierre).getDiferencia()).isEqualByComparingTo("0.00");
        assertThat(sesionCajaRepository.findAllByCompanyIdAndEstado(sedeA.getId(),
                veterinaria.vargasvet.domain.enums.EstadoSesionCaja.ABIERTA)).hasSize(1);

        // Quien no es administrador no puede cerrar la caja de otra persona
        comoPersonaConEquipo(ana, sedeA, RolePurpose.CUSTOM, equipoUno);
        ArqueoCajaRequest ajeno = new ArqueoCajaRequest();
        ajeno.setCompanyId(sedeA.getId());
        ajeno.setEfectivoContado(new BigDecimal("1.00"));
        ajeno.setSesionId(sesionDeLuis);
        assertThatThrownBy(() -> cajaService.cerrarCaja(ajeno))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    // ---------- Helpers de dominio adicionales ----------

    private HorarioEmpleado crearHorario(Empleado empleado, LocalDate fecha) {
        HorarioEmpleado h = new HorarioEmpleado();
        h.setEmpleado(empleado);
        h.setFecha(fecha);
        h.setDiaSemana(DiaSemana.LUNES);
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFin(LocalTime.of(12, 0));
        h.setActivo(true);
        return horarioEmpleadoRepository.save(h);
    }

    private Cita crearCitaCanceladaConPago(Company company) {
        Usuario duenoUsuario = crearIdentidad("dueno-" + company.getId() + "@example.test",
                "dueno." + company.getId(), "9" + company.getId() + "000001");
        Apoderado apoderado = crearApoderado(duenoUsuario, company);

        veterinaria.vargasvet.domain.entity.Raza raza = new veterinaria.vargasvet.domain.entity.Raza();
        raza.setNombre("Raza " + company.getId());
        raza.setEspecie(veterinaria.vargasvet.domain.enums.EspecieMascota.PERRO);
        raza = razaRepository.save(raza);

        veterinaria.vargasvet.domain.entity.Mascota mascota = new veterinaria.vargasvet.domain.entity.Mascota();
        mascota.setApoderado(apoderado);
        mascota.setNombreCompleto("Mascota " + company.getId());
        mascota.setEspecie(veterinaria.vargasvet.domain.enums.EspecieMascota.PERRO);
        mascota.setSexo(veterinaria.vargasvet.domain.enums.SexoMascota.MACHO);
        mascota.setActivo(true);
        mascota.setUuid(java.util.UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario vetUsuario = crearIdentidad("vet-" + company.getId() + "@example.test",
                "vet." + company.getId(), "8" + company.getId() + "000001");
        Empleado empleado = crearEmpleado(vetUsuario, company);

        veterinaria.vargasvet.domain.entity.ServiciosVeterinarios servicio =
                new veterinaria.vargasvet.domain.entity.ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta " + company.getId());
        servicio.setDescripcion("Consulta general de prueba");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDuracionEstimada(30);
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(LocalDateTime.now().minusDays(1));
        cita.setFechaHoraFin(LocalDateTime.now().minusDays(1).plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(new BigDecimal("100.00"));
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        Cita savedCita = citaRepository.save(cita);

        Purchase purchase = new Purchase();
        purchase.setCita(savedCita);
        purchase.setMetodoPago(MetodoPago.EFECTIVO);
        purchase.setTotal(new BigDecimal("100.00"));
        purchase.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        purchase.setPaymentStatus(PaymentStatus.PAID);
        purchase.setCreatedAt(LocalDateTime.now().minusDays(1));
        purchaseRepository.save(purchase);

        return savedCita;
    }

    // ---------- Escenarios 13 a 16: continuidad de la administración y roles por relación ----------

    private Role crearRolAdministradorGlobal(String nombre) {
        Role role = new Role();
        role.setName(nombre);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(RolePurpose.COMPANY_ADMIN);
        role.setActivo(true);
        return roleRepository.save(role);
    }

    private void autenticarComo(Integer companyId, RolePurpose purpose) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "actor@example.test", "n/a", List.of(), companyId,
                2, RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Usuario crearPersonaDeLaClinica(Company clinica, String correo, String username, String dni) {
        Usuario usuario = crearIdentidad(correo, username, dni);
        usuario.setCompany(clinica);
        usuarioRepository.save(usuario);
        crearCredencial(usuario, clinica, "ClaveA123!");
        return usuario;
    }

    @Test
    @DisplayName("[Escenario 13] Un empleado suspendido que sigue siendo cliente entra solo con su rol de cliente, y viceversa")
    void escenario13_cadaRolDependeDeSuPropiaRelacion() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-13");
        Role admin = crearRolAdministradorGlobal("ROLE_ADMIN_13");
        Role cliente = crearRolCliente(clinica);
        Usuario x = crearPersonaDeLaClinica(clinica, "x13@example.test", "x.trece", "13131313");
        crearEmpleado(x, clinica);
        asignarRol(x, admin, clinica);
        Usuario marco = crearPersonaDeLaClinica(clinica, "marco13@example.test", "marco.trece", "14141414");
        Empleado empleadoMarco = crearEmpleado(marco, clinica);
        Apoderado clienteMarco = crearApoderado(marco, clinica);
        asignarRol(marco, admin, clinica);
        asignarRol(marco, cliente, clinica);
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());

        empleadoService.cambiarEstado(empleadoMarco.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "Renunció");
        usuarioRepository.flush();

        AuthResponse comoCliente = usuarioService.loginWithGoogle("marco13@example.test", "clinica-a-13");
        assertThat(comoCliente.getRoles()).containsExactly("ROLE_CLIENTE");
        assertThat(comoCliente.getAssignedRoles()).containsExactly("ROLE_CLIENTE");
        assertThat(comoCliente.getAvailableRoles()).extracting(role -> role.getName()).containsExactly("ROLE_CLIENTE");

        empleadoService.cambiarEstado(empleadoMarco.getId(), true, null, null);
        apoderadoService.cambiarEstado(clienteMarco.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x");
        usuarioRepository.flush();

        AuthResponse comoPersonal = usuarioService.loginWithGoogle("marco13@example.test", "clinica-a-13");
        assertThat(comoPersonal.getRoles()).containsExactly("ROLE_ADMIN_13");
        assertThat(comoPersonal.getAssignedRoles()).containsExactly("ROLE_ADMIN_13");
        assertThat(comoPersonal.getAvailableRoles()).extracting(role -> role.getName()).containsExactly("ROLE_ADMIN_13");

        empleadoService.cambiarEstado(empleadoMarco.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x");
        usuarioRepository.flush();
        assertThatThrownBy(() -> usuarioService.loginWithGoogle("marco13@example.test", "clinica-a-13"))
                .isInstanceOf(veterinaria.vargasvet.exception.GoogleAccountSuspendedException.class);
    }

    @Test
    @DisplayName("[Escenario 14] Un administrador que ya no trabaja en la clínica no cuenta como reemplazo del único administrador")
    void escenario14_unClienteNoCuentaComoReemplazoDelUnicoAdministrador() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-14");
        Role admin = crearRolAdministradorGlobal("ROLE_ADMIN_14");
        Role cliente = crearRolCliente(clinica);
        Role recepcion = crearRol(clinica, "ROLE_RECEPCION_14");
        Usuario lucia = crearPersonaDeLaClinica(clinica, "lucia14@example.test", "lucia.catorce", "15151515");
        Empleado empleadoLucia = crearEmpleado(lucia, clinica);
        asignarRol(lucia, admin, clinica);
        Usuario marco = crearPersonaDeLaClinica(clinica, "marco14@example.test", "marco.catorce", "16161616");
        Empleado empleadoMarco = crearEmpleado(marco, clinica);
        crearApoderado(marco, clinica);
        asignarRol(marco, admin, clinica);
        asignarRol(marco, cliente, clinica);
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());
        empleadoService.cambiarEstado(empleadoMarco.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "Renunció");
        usuarioRepository.flush();

        veterinaria.vargasvet.dto.request.EmpleadoRequest quitarseElRol = new veterinaria.vargasvet.dto.request.EmpleadoRequest();
        quitarseElRol.setRoleIds(java.util.Set.of(recepcion.getId()));
        assertThatThrownBy(() -> empleadoService.updateEmpleado(empleadoLucia.getId(), quitarseElRol))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");
        assertThat(usuarioPorRolRepository.findByUsuarioId(lucia.getId()))
                .extracting(asignacion -> asignacion.getRol().getName()).containsExactly("ROLE_ADMIN_14");

        empleadoService.cambiarEstado(empleadoMarco.getId(), true, null, null);
        empleadoService.updateEmpleado(empleadoLucia.getId(), quitarseElRol);

        assertThat(usuarioPorRolRepository.findByUsuarioId(lucia.getId()))
                .extracting(asignacion -> asignacion.getRol().getName()).containsExactly("ROLE_RECEPCION_14");
    }

    @Test
    @DisplayName("[Escenario 15] Solo un administrador cambia el estado de un cliente que también es administrador")
    void escenario15_elEstadoDeUnClienteAdministradorLoCambiaSoloUnAdministrador() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-15");
        Role admin = crearRolAdministradorGlobal("ROLE_ADMIN_15");
        Role cliente = crearRolCliente(clinica);
        Usuario lucia = crearPersonaDeLaClinica(clinica, "lucia15@example.test", "lucia.quince", "17171717");
        crearEmpleado(lucia, clinica);
        Apoderado clienteLucia = crearApoderado(lucia, clinica);
        asignarRol(lucia, admin, clinica);
        asignarRol(lucia, cliente, clinica);
        usuarioRepository.flush();

        autenticarComo(clinica.getId(), RolePurpose.CUSTOM);
        assertThatThrownBy(() -> apoderadoService.cambiarEstado(clienteLucia.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(apoderadoRepository.findById(clienteLucia.getId()).orElseThrow().getEstado()).isTrue();

        autenticarComoAdministrador(clinica.getId());
        apoderadoService.cambiarEstado(clienteLucia.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x");
        assertThat(apoderadoRepository.findById(clienteLucia.getId()).orElseThrow().getEstado()).isFalse();
    }

    @Test
    @DisplayName("[Escenario 16] Editar a un administrador conserva su rol, y la lectura no entrega roles de otra clínica")
    void escenario16_editarAdministradoresYPersonasConRolesEnOtraClinica() {
        Company clinicaA = crearEmpresa("Clinica A", "clinica-a-16");
        Company clinicaB = crearEmpresa("Clinica B", "clinica-b-16");
        Role admin = crearRolAdministradorGlobal("ROLE_ADMIN_16");
        Role veterinario = crearRol(clinicaA, "ROLE_VETERINARIO_16");
        Role clienteB = crearRolCliente(clinicaB);
        Usuario marco = crearPersonaDeLaClinica(clinicaA, "marco16@example.test", "marco.dieciseis", "18181818");
        Empleado empleadoMarco = crearEmpleado(marco, clinicaA);
        asignarRol(marco, admin, clinicaA);

        Usuario ana = crearIdentidad("ana16@example.test", "ana.dieciseis", "19191919");
        crearCredencial(ana, clinicaA, "ClaveA123!");
        crearCredencial(ana, clinicaB, "ClaveB123!");
        Empleado empleadoAna = crearEmpleado(ana, clinicaA);
        crearApoderado(ana, clinicaB);
        asignarRol(ana, veterinario, clinicaA);
        asignarRol(ana, clienteB, clinicaB);
        new CompanyMembershipServiceImpl(empleadoRepository, apoderadoRepository, usuarioMembresiaRepository,
                usuarioRepository, companyRepository).syncLegacyCompanyField(ana);
        usuarioRepository.flush();

        autenticarComoAdministrador(clinicaA.getId());
        veterinaria.vargasvet.dto.request.EmpleadoRequest deMarco = empleadoService.findById(empleadoMarco.getId());
        assertThat(deMarco.getRoleIds()).containsExactly(admin.getId());
        deMarco.setNombre("Marco Corregido");
        empleadoService.updateEmpleado(empleadoMarco.getId(), deMarco);
        assertThat(usuarioRepository.findById(marco.getId()).orElseThrow().getNombre()).isEqualTo("Marco Corregido");
        assertThat(usuarioPorRolRepository.findByUsuarioId(marco.getId()))
                .extracting(asignacion -> asignacion.getRol().getName()).containsExactly("ROLE_ADMIN_16");

        veterinaria.vargasvet.dto.request.EmpleadoRequest deAna = empleadoService.findById(empleadoAna.getId());
        assertThat(deAna.getRoleIds()).containsExactly(veterinario.getId());
        deAna.setNombre("Ana Corregida");
        empleadoService.updateEmpleado(empleadoAna.getId(), deAna);
        assertThat(usuarioPorRolRepository.findByUsuarioId(ana.getId()))
                .extracting(asignacion -> asignacion.getRol().getName())
                .containsExactlyInAnyOrder("ROLE_VETERINARIO_16", "ROLE_CLIENTE");

        autenticarComo(clinicaA.getId(), RolePurpose.CUSTOM);
        veterinaria.vargasvet.dto.request.EmpleadoRequest sinCambiarRoles = empleadoService.findById(empleadoMarco.getId());
        sinCambiarRoles.setNombre("Marco Otra Vez");
        empleadoService.updateEmpleado(empleadoMarco.getId(), sinCambiarRoles);
        veterinaria.vargasvet.dto.request.EmpleadoRequest cambiandoRoles = empleadoService.findById(empleadoMarco.getId());
        cambiandoRoles.setRoleIds(java.util.Set.of(veterinario.getId()));
        assertThatThrownBy(() -> empleadoService.updateEmpleado(empleadoMarco.getId(), cambiandoRoles))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(usuarioPorRolRepository.findByUsuarioId(marco.getId()))
                .extracting(asignacion -> asignacion.getRol().getName()).containsExactly("ROLE_ADMIN_16");
    }

    // ---------- Escenarios 17 a 20: la baja y sus protecciones ----------

    private Cita crearCitaDe(Company company, Empleado vet, Apoderado dueno, EstadoCita estado, LocalDateTime inicio,
                             String sufijo) {
        veterinaria.vargasvet.domain.entity.Mascota mascota = new veterinaria.vargasvet.domain.entity.Mascota();
        mascota.setApoderado(dueno);
        mascota.setNombreCompleto("Mascota " + sufijo);
        mascota.setEspecie(veterinaria.vargasvet.domain.enums.EspecieMascota.PERRO);
        mascota.setSexo(veterinaria.vargasvet.domain.enums.SexoMascota.MACHO);
        mascota.setActivo(true);
        mascota.setUuid(java.util.UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);
        veterinaria.vargasvet.domain.entity.ServiciosVeterinarios servicio =
                new veterinaria.vargasvet.domain.entity.ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta " + sufijo);
        servicio.setDescripcion("Consulta");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDuracionEstimada(30);
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio = serviciosVeterinariosRepository.save(servicio);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(vet);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    @Test
    @DisplayName("[Escenario 17] No se da de baja a quien tiene una cita en curso o dentro de su horario")
    void escenario17_laBajaConsideraLasCitasQueYaEmpezaron() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-17");
        Usuario duenoU = crearPersonaDeLaClinica(clinica, "dueno17@example.test", "dueno.diecisiete", "81000001");
        Apoderado dueno = crearApoderado(duenoU, clinica);
        Usuario duenoU2 = crearPersonaDeLaClinica(clinica, "dueno17b@example.test", "dueno.diecisieteb", "81000002");
        Apoderado dueno2 = crearApoderado(duenoU2, clinica);
        Empleado enConsulta = crearEmpleado(crearPersonaDeLaClinica(clinica, "v1-17@example.test", "v.uno17", "82000001"), clinica);
        Empleado enHorario = crearEmpleado(crearPersonaDeLaClinica(clinica, "v2-17@example.test", "v.dos17", "82000002"), clinica);
        Empleado enEspera = crearEmpleado(crearPersonaDeLaClinica(clinica, "v3-17@example.test", "v.tres17", "82000003"), clinica);
        Empleado libre = crearEmpleado(crearPersonaDeLaClinica(clinica, "v4-17@example.test", "v.cuatro17", "82000004"), clinica);
        LocalDateTime ahora = LocalDateTime.now();
        crearCitaDe(clinica, enConsulta, dueno, EstadoCita.EN_PROCESO, ahora.minusMinutes(10), "a");
        crearCitaDe(clinica, enHorario, dueno, EstadoCita.PROGRAMADA, ahora.minusMinutes(10), "b");
        crearCitaDe(clinica, enEspera, dueno, EstadoCita.SALA_DE_ESPERA, ahora.minusMinutes(50), "c");
        crearCitaDe(clinica, libre, dueno2, EstadoCita.PROGRAMADA, ahora.minusHours(3), "d");
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());

        for (Empleado ocupado : List.of(enConsulta, enHorario, enEspera)) {
            assertThatThrownBy(() -> empleadoService.cambiarEstado(ocupado.getId(), false,
                    veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("citas programadas vigentes");
            assertThat(empleadoRepository.findById(ocupado.getId()).orElseThrow().getEstado()).isTrue();
        }
        assertThatThrownBy(() -> apoderadoService.cambiarEstado(dueno.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("citas programadas vigentes");

        empleadoService.cambiarEstado(libre.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        apoderadoService.cambiarEstado(dueno2.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        assertThat(empleadoRepository.findById(libre.getId()).orElseThrow().getEstado()).isFalse();
        assertThat(apoderadoRepository.findById(dueno2.getId()).orElseThrow().getEstado()).isFalse();
    }

    @Test
    @DisplayName("[Escenario 18] Solo un administrador elimina; eliminar es una baja y conserva los registros")
    void escenario18_eliminarEsDeAdministradoresYConservaLosRegistros() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-18");
        Role cliente = crearRolCliente(clinica);
        Usuario vetU = crearPersonaDeLaClinica(clinica, "vet18@example.test", "vet.dieciocho", "83000001");
        Empleado vet = crearEmpleado(vetU, clinica);
        Usuario clienteU = crearPersonaDeLaClinica(clinica, "cliente18@example.test", "cliente.dieciocho", "83000002");
        Apoderado ap = crearApoderado(clienteU, clinica);
        asignarRol(clienteU, cliente, clinica);
        usuarioRepository.flush();

        autenticarComo(clinica.getId(), RolePurpose.CUSTOM);
        assertThatThrownBy(() -> empleadoService.eliminar(vet.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> apoderadoService.eliminar(ap.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(empleadoRepository.findById(vet.getId()).orElseThrow().getEstado()).isTrue();
        assertThat(apoderadoRepository.findById(ap.getId()).orElseThrow().getEstado()).isTrue();

        autenticarComoAdministrador(clinica.getId());
        empleadoService.eliminar(vet.getId());
        apoderadoService.eliminar(ap.getId());

        Empleado empleadoDespues = empleadoRepository.findById(vet.getId()).orElseThrow();
        assertThat(empleadoDespues.getEstado()).isFalse();
        assertThat(empleadoDespues.getTipoInactividad()).isEqualTo(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        Apoderado clienteDespues = apoderadoRepository.findById(ap.getId()).orElseThrow();
        assertThat(clienteDespues.getEstado()).isFalse();
        assertThat(clienteDespues.getFechaSalida()).isNotNull();
        assertThat(usuarioRepository.existsById(vetU.getId())).isTrue();
        assertThat(usuarioRepository.existsById(clienteU.getId())).isTrue();
    }

    @Test
    @DisplayName("[Escenario 19] La baja de quien dejó una caja abierta se aplica, avisa y un administrador cierra esa caja")
    void escenario19_laBajaConCajaAbiertaAvisaYUnAdministradorLaCierra() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-19");
        Usuario carlaU = crearPersonaDeLaClinica(clinica, "carla19@example.test", "carla.diecinueve", "84000001");
        Empleado carla = crearEmpleado(carlaU, clinica);
        Usuario sinCajaU = crearPersonaDeLaClinica(clinica, "sin19@example.test", "sin.diecinueve", "84000002");
        Empleado sinCaja = crearEmpleado(sinCajaU, clinica);
        veterinaria.vargasvet.domain.entity.Caja mostrador = new veterinaria.vargasvet.domain.entity.Caja();
        mostrador.setCompanyId(clinica.getId());
        mostrador.setNombre("Mostrador 1");
        mostrador.setActiva(true);
        mostrador.setCreadaAt(LocalDateTime.now());
        mostrador = cajaRepository.save(mostrador);
        veterinaria.vargasvet.domain.entity.SesionCaja sesion = new veterinaria.vargasvet.domain.entity.SesionCaja();
        sesion.setCompanyId(clinica.getId());
        sesion.setCajaId(mostrador.getId());
        sesion.setEstado(veterinaria.vargasvet.domain.enums.EstadoSesionCaja.ABIERTA);
        sesion.setMontoApertura(new BigDecimal("500.00"));
        sesion.setEfectivoEsperado(new BigDecimal("500.00"));
        sesion.setAbiertaAt(LocalDateTime.now().minusHours(2));
        sesion.setAbiertaPor(carlaU.getEmail());
        sesion.setAbiertaPorUsuarioId(carlaU.getId());
        sesion = sesionCajaRepository.save(sesion);
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());

        List<String> cajas = empleadoService.cambiarEstado(carla.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Renuncia");

        assertThat(cajas).containsExactly("Mostrador 1");
        assertThat(empleadoRepository.findById(carla.getId()).orElseThrow().getEstado()).isFalse();
        assertThat(sesionCajaRepository.findById(sesion.getId()).orElseThrow().getEstado())
                .isEqualTo(veterinaria.vargasvet.domain.enums.EstadoSesionCaja.ABIERTA);
        assertThat(empleadoService.cambiarEstado(sinCaja.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x")).isEmpty();

        ArqueoCajaRequest cierre = new ArqueoCajaRequest();
        cierre.setCompanyId(clinica.getId());
        cierre.setSesionId(sesion.getId());
        cierre.setEfectivoContado(new BigDecimal("500.00"));
        cajaService.cerrarCaja(cierre);

        veterinaria.vargasvet.domain.entity.SesionCaja cerrada = sesionCajaRepository.findById(sesion.getId()).orElseThrow();
        assertThat(cerrada.getEstado()).isEqualTo(veterinaria.vargasvet.domain.enums.EstadoSesionCaja.CERRADA);
        assertThat(cerrada.getCerradaPor()).isEqualTo("admin@example.test");
    }

    @Test
    @DisplayName("[Escenario 20] Repetir la misma baja o reactivación no repite sus efectos")
    void escenario20_repetirLaMismaAccionNoRepiteSusEfectos() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-20");
        Role cliente = crearRolCliente(clinica);
        Empleado vet = crearEmpleado(crearPersonaDeLaClinica(clinica, "vet20@example.test", "vet.veinte", "85000001"), clinica);
        Usuario clienteU = crearPersonaDeLaClinica(clinica, "cliente20@example.test", "cliente.veinte", "85000002");
        Apoderado ap = crearApoderado(clienteU, clinica);
        asignarRol(clienteU, cliente, clinica);
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());

        empleadoService.cambiarEstado(vet.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Primera");
        LocalDateTime primeraVez = empleadoRepository.findById(vet.getId()).orElseThrow().getFechaModificacionEstado();
        empleadoService.cambiarEstado(vet.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Segunda");
        Empleado tras = empleadoRepository.findById(vet.getId()).orElseThrow();
        assertMismoInstante(tras.getFechaModificacionEstado(), primeraVez);

        tras.setFechaModificacionEstado(LocalDateTime.now().minusMinutes(10));
        empleadoRepository.save(tras);
        assertThatThrownBy(() -> empleadoService.cambiarEstado(vet.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Tercera"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solo puede reactivarse");

        apoderadoService.cambiarEstado(ap.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Primera");
        LocalDateTime clientePrimera = apoderadoRepository.findById(ap.getId()).orElseThrow().getFechaModificacionEstado();
        apoderadoService.cambiarEstado(ap.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "Segunda");
        assertMismoInstante(apoderadoRepository.findById(ap.getId()).orElseThrow().getFechaModificacionEstado(), clientePrimera);

        empleadoService.cambiarEstado(vet.getId(), true, null, null);
        LocalDateTime reactivado = empleadoRepository.findById(vet.getId()).orElseThrow().getFechaModificacionEstado();
        empleadoService.cambiarEstado(vet.getId(), true, null, null);
        assertMismoInstante(empleadoRepository.findById(vet.getId()).orElseThrow().getFechaModificacionEstado(), reactivado);
    }

    private void assertMismoInstante(LocalDateTime actual, LocalDateTime esperado) {
        assertThat(actual).isCloseTo(esperado, org.assertj.core.api.Assertions.within(1L, java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("[Escenario 21] Dar de baja a un cliente no le quita su empresa, y solo vuelve reactivándolo, no por el registro")
    void escenario21_laBajaConservaLaEmpresaYSoloSeRevierteReactivando() {
        Company a = crearEmpresa("Clinica A", "clinica-a-21");
        Company b = crearEmpresa("Clinica B", "clinica-b-21");
        Role rolA = crearRolCliente(a);
        Role rolB = crearRolCliente(b);
        Usuario enA = crearPersonaDeLaClinica(a, "ana21@example.test", "ana.a21", "86000001");
        Usuario enB = crearPersonaDeLaClinica(b, "ana21@example.test", "ana.b21", "86000002");
        Apoderado apA = crearApoderado(enA, a);
        Apoderado apB = crearApoderado(enB, b);
        asignarRol(enA, rolA, a);
        asignarRol(enB, rolB, b);
        usuarioRepository.flush();

        autenticarComoAdministrador(a.getId());
        apoderadoService.cambiarEstado(apA.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        autenticarComoAdministrador(b.getId());
        apoderadoService.cambiarEstado(apB.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        usuarioRepository.flush();

        assertThat(usuarioRepository.findById(enA.getId()).orElseThrow().getCompany().getId()).isEqualTo(a.getId());
        assertThat(usuarioRepository.findById(enB.getId()).orElseThrow().getCompany().getId()).isEqualTo(b.getId());

        autenticarComoAdministrador(a.getId());
        veterinaria.vargasvet.dto.request.ApoderadoRequest reingreso = new veterinaria.vargasvet.dto.request.ApoderadoRequest();
        reingreso.setNombre("Ana");
        reingreso.setApellido("Regresa");
        reingreso.setNumeroDocumento("86000001");
        reingreso.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        reingreso.setGenero(Genero.FEMENINO);
        reingreso.setEmail("ana21@example.test");
        reingreso.setUsername("ana.a21");
        reingreso.setTelefono("999999999");
        reingreso.setDireccion("Av. Prueba 123");
        reingreso.setRoleIds(java.util.Set.of(rolA.getId()));
        assertThatThrownBy(() -> apoderadoService.registerApoderado(reingreso))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dado de baja")
                .hasMessageContaining("Reactivar");
        assertThat(apoderadoRepository.findByCompanyId(a.getId())).hasSize(1);
        assertThat(apoderadoRepository.findById(apA.getId()).orElseThrow().getEstado()).isFalse();

        apoderadoService.cambiarEstado(apA.getId(), true, null, null);
        apoderadoRepository.flush();

        List<Apoderado> filasDeA = apoderadoRepository.findByCompanyId(a.getId());
        assertThat(filasDeA).hasSize(1);
        assertThat(filasDeA.get(0).getId()).isEqualTo(apA.getId());
        assertThat(filasDeA.get(0).getEstado()).isTrue();
        assertThat(filasDeA.get(0).getTipoInactividad()).isNull();
        assertThat(filasDeA.get(0).getFechaSalida()).isNull();
        assertThat(usuarioPorRolRepository.findByUsuarioId(enA.getId())).hasSize(1);
        assertThat(apoderadoRepository.findById(apB.getId()).orElseThrow().getEstado()).isFalse();
    }

    @Test
    @DisplayName("[Escenario 22] Un cliente suspendido no vuelve por el registro: se reactiva desde la lista")
    void escenario22_elRegistroNoLevantaUnaSuspension() {
        Company a = crearEmpresa("Clinica A", "clinica-a-22");
        Role rolA = crearRolCliente(a);
        Usuario u = crearPersonaDeLaClinica(a, "cliente22@example.test", "cliente.veintidos", "87000001");
        Apoderado ap = crearApoderado(u, a);
        asignarRol(u, rolA, a);
        usuarioRepository.flush();
        autenticarComoAdministrador(a.getId());
        apoderadoService.cambiarEstado(ap.getId(), false, veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x");

        veterinaria.vargasvet.dto.request.ApoderadoRequest registro = new veterinaria.vargasvet.dto.request.ApoderadoRequest();
        registro.setNombre("Cliente");
        registro.setApellido("Suspendido");
        registro.setNumeroDocumento("87000001");
        registro.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        registro.setGenero(Genero.MASCULINO);
        registro.setEmail("cliente22@example.test");
        registro.setUsername("cliente.veintidos");
        registro.setTelefono("999999999");
        registro.setDireccion("Av. Prueba 123");
        registro.setRoleIds(java.util.Set.of(rolA.getId()));

        assertThatThrownBy(() -> apoderadoService.registerApoderado(registro))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suspendido")
                .hasMessageContaining("Reactivar");
        assertThat(apoderadoRepository.findById(ap.getId()).orElseThrow().getEstado()).isFalse();
    }

    @Test
    @DisplayName("[Escenario 23] Un cliente con deuda de citas completadas no se da de baja; sí se puede suspender y, al saldar, dar de baja")
    void escenario23_noSeDaDeBajaAUnClienteConDeuda() {
        Company clinica = crearEmpresa("Clinica A", "clinica-a-23");
        Usuario deudorU = crearPersonaDeLaClinica(clinica, "deudor23@example.test", "deudor.veintitres", "88000001");
        Apoderado deudor = crearApoderado(deudorU, clinica);
        Usuario sinDeudaU = crearPersonaDeLaClinica(clinica, "sindeuda23@example.test", "sin.deuda23", "88000002");
        Apoderado sinDeuda = crearApoderado(sinDeudaU, clinica);
        Empleado vet = crearEmpleado(crearPersonaDeLaClinica(clinica, "vet23@example.test", "vet.veintitres", "88000003"), clinica);
        LocalDateTime ahora = LocalDateTime.now();
        Cita pendiente = crearCitaDe(clinica, vet, deudor, EstadoCita.COMPLETADA, ahora.minusDays(2), "a");
        pendiente.setMontoPagado(new BigDecimal("40.00"));
        citaRepository.save(pendiente);
        crearCitaDe(clinica, vet, sinDeuda, EstadoCita.CANCELADA, ahora.minusDays(2), "b");
        usuarioRepository.flush();
        autenticarComoAdministrador(clinica.getId());

        assertThatThrownBy(() -> apoderadoService.cambiarEstado(deudor.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deuda pendiente de S/ 60.00");
        assertThatThrownBy(() -> apoderadoService.eliminar(deudor.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deuda pendiente");
        assertThat(apoderadoRepository.findById(deudor.getId()).orElseThrow().getEstado()).isTrue();

        apoderadoService.cambiarEstado(deudor.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION, "x");
        assertThatThrownBy(() -> apoderadoService.cambiarEstado(deudor.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x"))
                .hasMessageContaining("deuda pendiente");
        assertThat(apoderadoRepository.findById(deudor.getId()).orElseThrow().getTipoInactividad())
                .isEqualTo(veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);

        pendiente.setMontoPagado(new BigDecimal("100.00"));
        citaRepository.save(pendiente);
        apoderadoService.cambiarEstado(deudor.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        apoderadoService.cambiarEstado(sinDeuda.getId(), false,
                veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA, "x");
        assertThat(apoderadoRepository.findById(deudor.getId()).orElseThrow().getTipoInactividad())
                .isEqualTo(veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
        assertThat(apoderadoRepository.findById(sinDeuda.getId()).orElseThrow().getEstado()).isFalse();
    }

    @Test
    @DisplayName("[Escenario 24] Un mismo navegador puede ser el punto de cobro de dos sedes sin que registrar una desvincule a la otra")
    void escenario24_elEquipoSeRegistraPorSede() {
        Company sedeA = crearEmpresa("Sede A", "sede-a-24");
        Company sedeB = crearEmpresa("Sede B", "sede-b-24");
        Usuario adminA = crearIdentidadDeEmpresa("admina24@example.test", "admin.a24", "24242410", sedeA);
        Usuario adminB = crearIdentidadDeEmpresa("adminb24@example.test", "admin.b24", "24242411", sedeB);
        veterinaria.vargasvet.security.CajaDispositivoCookie cookie = new veterinaria.vargasvet.security.CajaDispositivoCookie();
        veterinaria.vargasvet.service.PuntoCobroService puntos = new veterinaria.vargasvet.service.PuntoCobroService(
                cajaRepository, sesionCajaRepository, usuarioRepository, cookie, mock(AuditLogService.class), companyRepository);

        comoPersonaConEquipo(adminA, sedeA, RolePurpose.COMPANY_ADMIN, null);
        Long cajaA = puntos.listar(sedeA.getId()).get(0).id();
        org.springframework.mock.web.MockHttpServletResponse respuestaA = new org.springframework.mock.web.MockHttpServletResponse();
        puntos.vincular(sedeA.getId(), cajaA, new org.springframework.mock.web.MockHttpServletRequest(), respuestaA);
        String tokenA = tokenDe(respuestaA);
        assertThat(respuestaA.getHeader("Set-Cookie"))
                .startsWith(veterinaria.vargasvet.security.CajaDispositivoCookie.nombre("sede-a-24") + "=")
                .contains("Path=/api/v1;");

        comoPersonaConEquipo(adminB, sedeB, RolePurpose.COMPANY_ADMIN, null);
        Long cajaB = puntos.listar(sedeB.getId()).get(0).id();
        org.springframework.mock.web.MockHttpServletRequest navegador = new org.springframework.mock.web.MockHttpServletRequest();
        navegador.setCookies(new jakarta.servlet.http.Cookie(
                veterinaria.vargasvet.security.CajaDispositivoCookie.nombre("sede-a-24"), tokenA));
        org.springframework.mock.web.MockHttpServletResponse respuestaB = new org.springframework.mock.web.MockHttpServletResponse();
        puntos.vincular(sedeB.getId(), cajaB, navegador, respuestaB);
        String tokenB = tokenDe(respuestaB);

        assertThat(respuestaB.getHeader("Set-Cookie"))
                .startsWith(veterinaria.vargasvet.security.CajaDispositivoCookie.nombre("sede-b-24") + "=");
        assertThat(cajaRepository.findById(cajaA).orElseThrow().getDispositivoTokenHash()).isNotNull();
        assertThat(cajaRepository.findById(cajaB).orElseThrow().getDispositivoTokenHash()).isNotNull();

        org.springframework.mock.web.MockHttpServletRequest conAmbas = new org.springframework.mock.web.MockHttpServletRequest();
        conAmbas.setCookies(
                new jakarta.servlet.http.Cookie(veterinaria.vargasvet.security.CajaDispositivoCookie.nombre("sede-a-24"), tokenA),
                new jakarta.servlet.http.Cookie(veterinaria.vargasvet.security.CajaDispositivoCookie.nombre("sede-b-24"), tokenB));
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(conAmbas));
        comoPersonaConEquipo(adminA, sedeA, RolePurpose.COMPANY_ADMIN, null);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(conAmbas));
        assertThat(puntos.resolver(sedeA.getId()).modo()).isEqualTo("DISPOSITIVO");
        assertThat(puntos.resolver(sedeA.getId()).caja().getId()).isEqualTo(cajaA);

        autenticarComoAdministrador(sedeB.getId());
        assertThat(puntos.resolver(sedeB.getId()).modo()).isEqualTo("DISPOSITIVO");
        assertThat(puntos.resolver(sedeB.getId()).caja().getId()).isEqualTo(cajaB);
    }
}
