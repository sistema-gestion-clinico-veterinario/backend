package veterinaria.vargasvet.ers.clientes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.SexoMascota;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.mapper.MascotaMapper;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RazaRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.impl.ApoderadoServiceImpl;
import veterinaria.vargasvet.service.impl.MascotaServiceImpl;
import veterinaria.vargasvet.util.BusinessValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF17To21Test {

    private UsuarioRepository usuarioRepository;
    private ApoderadoRepository apoderadoRepository;
    private MascotaRepository mascotaRepository;
    private HistoriaClinicaRepository historiaRepository;
    private RoleRepository roleRepository;
    private CompanyRepository companyRepository;
    private RazaRepository razaRepository;
    private UserMapper userMapper;
    private MascotaMapper mascotaMapper;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        apoderadoRepository = mock(ApoderadoRepository.class);
        mascotaRepository = mock(MascotaRepository.class);
        historiaRepository = mock(HistoriaClinicaRepository.class);
        roleRepository = mock(RoleRepository.class);
        companyRepository = mock(CompanyRepository.class);
        razaRepository = mock(RazaRepository.class);
        userMapper = mock(UserMapper.class);
        mascotaMapper = mock(MascotaMapper.class);
        emailService = mock(EmailService.class);
        autenticarEmpresa(7);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF17-01] Lista clientes filtrados, paginados y limitados a la empresa")
    void cpRf1701_listaClientesDeLaEmpresa() {
        Apoderado ana = apoderado(10L, 7, true, "Ana", "Torres", "12345678");
        when(apoderadoRepository.buscar(eq(7), eq("Ana Torres"), eq("1234"), any()))
                .thenReturn(new PageImpl<>(List.of(ana)));

        var page = apoderadoService().listar(null, "  Ana   Torres ", "1234", 0, 5);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(page.getContent().get(0).getNombre()).isEqualTo("Ana");
        verify(apoderadoRepository).buscar(eq(7), eq("Ana Torres"), eq("1234"), any());
    }

    @Test
    @DisplayName("[CP-RF18-01] Registra cliente inactivo con activación temporal y rol de su empresa")
    void cpRf1801_registraClienteConActivacionTemporal() {
        Company company = company(7);
        Role role = roleCliente(20, company);
        ApoderadoRequest request = apoderadoRequest(7, role.getId());
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(roleRepository.findAllById(Set.of(20))).thenReturn(List.of(role));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(30);
            return user;
        });
        when(apoderadoRepository.save(any(Apoderado.class))).thenAnswer(invocation -> {
            Apoderado value = invocation.getArgument(0);
            value.setId(40L);
            return value;
        });
        when(userMapper.toProfileDTO(any(Usuario.class))).thenReturn(new UserProfileDTO());
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());

        UserProfileDTO response = apoderadoService().registerApoderado(request);

        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());
        Usuario created = userCaptor.getAllValues().get(0);
        assertThat(created.isActivo()).isFalse();
        assertThat(created.isEmailVerified()).isFalse();
        assertThat(created.getVerificationToken()).isNotBlank();
        assertThat(created.getVerificationTokenExpiresAt()).isAfter(java.time.LocalDateTime.now());
        assertThat(created.getCompany().getId()).isEqualTo(7);
        assertThat(created.getUsuariosPorRol()).extracting(assignment -> assignment.getRol().getId())
                .containsExactly(20);
        assertThat(response.getApoderadoId()).isEqualTo(40);
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/welcome-template"));
    }

    @Test
    @DisplayName("[CP-RF18-02] Rechaza correo duplicado antes de crear usuario o apoderado")
    void cpRf1802_rechazaClienteDuplicadoSinRegistrosParciales() {
        ApoderadoRequest request = apoderadoRequest(7, 20);
        when(usuarioRepository.existsByEmail("ana.qa@example.test")).thenReturn(true);

        assertThatThrownBy(() -> apoderadoService().registerApoderado(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email ya está registrado");

        verify(usuarioRepository, never()).save(any());
        verify(apoderadoRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), anyString());
    }

    @Test
    @DisplayName("[CP-RF20-01] Lista únicamente mascotas filtradas de la empresa autenticada")
    void cpRf2001_listaMascotasAutorizadasConFiltros() {
        Mascota luna = mascota(50L, apoderado(10L, 7, true, "Ana", "Torres", "12345678"));
        MascotaResponse mapped = new MascotaResponse();
        mapped.setId(50L);
        mapped.setNombreCompleto("Luna");
        when(mascotaRepository.buscar(eq(7), eq("Luna"), eq(EspecieMascota.PERRO), eq("Ana"), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(luna)));
        when(mascotaMapper.toResponse(luna)).thenReturn(mapped);

        var page = mascotaService().listar(null, " Luna ", EspecieMascota.PERRO, " Ana ", true, 0, 10);

        assertThat(page.getContent()).extracting(MascotaResponse::getId).containsExactly(50L);
        verify(mascotaRepository).buscar(eq(7), eq("Luna"), eq(EspecieMascota.PERRO), eq("Ana"), eq(true), any());
    }

    @Test
    @DisplayName("[CP-RF21-01] Registra mascota vinculada al cliente permitiendo peso y foto vacíos")
    void cpRf2101_registraMascotaSinPesoNiFoto() {
        Apoderado owner = apoderado(10L, 7, true, "Ana", "Torres", "12345678");
        MascotaRequest request = mascotaRequest(10L);
        Raza raza = new Raza();
        raza.setId(5L);
        when(apoderadoRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(razaRepository.findById(5L)).thenReturn(Optional.of(raza));
        when(mascotaRepository.save(any(Mascota.class))).thenAnswer(invocation -> {
            Mascota value = invocation.getArgument(0);
            value.setId(50L);
            return value;
        });
        MascotaResponse mapped = new MascotaResponse();
        mapped.setId(50L);
        when(mascotaMapper.toResponse(any(Mascota.class))).thenReturn(mapped);

        MascotaResponse response = mascotaService().registerMascota(request);

        ArgumentCaptor<Mascota> mascotaCaptor = ArgumentCaptor.forClass(Mascota.class);
        verify(mascotaRepository).save(mascotaCaptor.capture());
        assertThat(mascotaCaptor.getValue().getApoderado()).isSameAs(owner);
        assertThat(mascotaCaptor.getValue().getPeso()).isNull();
        assertThat(mascotaCaptor.getValue().getFotoUrl()).isNull();
        assertThat(mascotaCaptor.getValue().getUuid()).isNotBlank();
        ArgumentCaptor<HistoriaClinica> historiaCaptor = ArgumentCaptor.forClass(HistoriaClinica.class);
        verify(historiaRepository).save(historiaCaptor.capture());
        assertThat(historiaCaptor.getValue().getMascota().getId()).isEqualTo(50L);
        assertThat(response.getId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("[CP-RF21-02] Rechaza mascota con propietario inactivo sin crear relaciones")
    void cpRf2102_rechazaPropietarioInactivoSinPersistir() {
        Apoderado owner = apoderado(10L, 7, false, "Ana", "Torres", "12345678");
        when(apoderadoRepository.findById(10L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> mascotaService().registerMascota(mascotaRequest(10L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueño inactivo");

        verify(mascotaRepository, never()).save(any());
        verify(historiaRepository, never()).save(any());
    }

    private ApoderadoServiceImpl apoderadoService() {
        ApoderadoServiceImpl service = new ApoderadoServiceImpl(
                usuarioRepository,
                apoderadoRepository,
                mascotaRepository,
                mock(RefreshTokenRepository.class),
                mock(UsuarioPorRolRepository.class),
                roleRepository,
                companyRepository,
                mock(PasswordEncoder.class, invocation -> "hash-temporal"),
                userMapper,
                mock(BusinessValidator.class),
                emailService,
                mock(AuditLogService.class),
                mock(CompanyRoleProvisioningService.class),
                mock(SessionSecurityService.class)
        );
        ReflectionTestUtils.setField(service, "frontendVerifyUrl", "https://frontend.test/activar?token=");
        ReflectionTestUtils.setField(service, "defaultCompanyName", "Veterinaria QA");
        ReflectionTestUtils.setField(service, "defaultCompanyLogo", "");
        ReflectionTestUtils.setField(service, "companyEmail", "qa@example.test");
        ReflectionTestUtils.setField(service, "companyPhone", "999999999");
        ReflectionTestUtils.setField(service, "companyAddress", "Dirección QA");
        ReflectionTestUtils.setField(service, "verificationTokenValidityHours", 24L);
        return service;
    }

    private MascotaServiceImpl mascotaService() {
        return new MascotaServiceImpl(
                mascotaRepository,
                apoderadoRepository,
                mock(CitaRepository.class),
                historiaRepository,
                mascotaMapper,
                mock(BusinessValidator.class),
                mock(AuditLogService.class),
                razaRepository
        );
    }

    private ApoderadoRequest apoderadoRequest(Integer companyId, Integer roleId) {
        ApoderadoRequest request = new ApoderadoRequest();
        request.setNombre("Ana");
        request.setApellido("Prueba");
        request.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        request.setNumeroDocumento("12345678");
        request.setEmail("ana.qa@example.test");
        request.setTelefono("999999999");
        request.setDireccion("Avenida Prueba 123");
        request.setGenero(Genero.FEMENINO);
        request.setCompanyId(companyId);
        request.setRoleIds(Set.of(roleId));
        return request;
    }

    private MascotaRequest mascotaRequest(Long apoderadoId) {
        MascotaRequest request = new MascotaRequest();
        request.setNombreCompleto("Luna");
        request.setEspecie(EspecieMascota.PERRO);
        request.setRazaId(5L);
        request.setSexo(SexoMascota.HEMBRA);
        request.setFechaNacimiento(LocalDate.now().minusYears(2));
        request.setPeso(null);
        request.setFotoUrl(null);
        request.setApoderadoId(apoderadoId);
        return request;
    }

    private Apoderado apoderado(Long id, Integer companyId, boolean active, String nombre,
                                String apellido, String documento) {
        Usuario user = new Usuario();
        user.setId(id.intValue());
        user.setNombre(nombre);
        user.setApellido(apellido);
        user.setActivo(active);
        user.setCompany(company(companyId));
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setUser(user);
        apoderado.setNumeroDocumento(documento);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setGenero(Genero.FEMENINO);
        return apoderado;
    }

    private Mascota mascota(Long id, Apoderado owner) {
        Mascota mascota = new Mascota();
        mascota.setId(id);
        mascota.setNombreCompleto("Luna");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(owner);
        return mascota;
    }

    private Company company(Integer id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Empresa QA " + id);
        company.setActivo(true);
        return company;
    }

    private Role roleCliente(Integer id, Company company) {
        Role role = new Role();
        role.setId(id);
        role.setName("APODERADO QA");
        role.setScope(RoleScope.CLIENT);
        role.setPurpose(RolePurpose.CLIENT_PORTAL);
        role.setActivo(true);
        role.setCompany(company);
        return role;
    }

    private void autenticarEmpresa(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(
                15,
                "qa@empresa.test",
                "",
                List.of(),
                companyId
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
