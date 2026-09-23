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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioServiceImpl usuarioService;
    private EmpleadoServiceImpl empleadoService;
    private ApoderadoServiceImpl apoderadoService;
    private CajaServiceImpl cajaService;

    @BeforeEach
    void setUp() {
        CompanyMembershipService companyMembershipService = new CompanyMembershipServiceImpl(
                empleadoRepository, apoderadoRepository, usuarioMembresiaRepository, usuarioRepository, companyRepository);
        SessionSecurityService sessionSecurityService = new SessionSecurityService(
                usuarioRepository, refreshTokenRepository, credencialRepository);
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
                mock(SharedRateLimitService.class), mock(AuthenticationAuditService.class),
                new PasswordPolicyService(), mock(LegalDocumentService.class), contactoService);

        empleadoService = new EmpleadoServiceImpl(
                usuarioRepository, roleRepository, empleadoRepository, especialidadRepository, tipoEmpleadoRepository,
                companyRepository, horarioEmpleadoRepository, companyOperatingHourRepository, companyExceptionRepository,
                citaRepository, passwordEncoder, userMapper, mock(EmailService.class), mock(BusinessValidator.class),
                mock(AuditLogService.class), usuarioPorRolRepository, sessionSecurityService, companyMembershipService,
                credencialRepository, contactoService);

        apoderadoService = new ApoderadoServiceImpl(
                usuarioRepository, apoderadoRepository, mascotaRepository,
                refreshTokenRepository, usuarioPorRolRepository, roleRepository, companyRepository, passwordEncoder,
                userMapper, mock(BusinessValidator.class), mock(EmailService.class), mock(AuditLogService.class),
                mock(veterinaria.vargasvet.service.CompanyRoleProvisioningService.class), sessionSecurityService,
                companyMembershipService, citaRepository, credencialRepository, contactoService);

        cajaService = new CajaServiceImpl(
                movimientoCajaRepository, citaRepository, purchaseRepository, sesionCajaRepository,
                mock(AuditLogService.class));
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
}
