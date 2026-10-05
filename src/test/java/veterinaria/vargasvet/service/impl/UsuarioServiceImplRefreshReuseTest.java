package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.exception.RefreshTokenReuseException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.RealtimeSubscriptionGuard;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.util.AppClock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dos pestañas de la misma clínica pueden renovar a la vez con la misma cookie. Un token recién rotado que vuelve a
 * presentarse dentro del margen de gracia no se toma por un robo; fuera del margen, o con la sesión ya cerrada, sí.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplRefreshReuseTest {

    private static final int COMPANY_ID = 3;
    private static final String FAMILIA = "familia-1";
    private static final String TOKEN = "refresh-viejo";

    @Mock UsuarioRepository usuarioRepository;
    @Mock CompanyRepository companyRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    @Mock TokenProvider tokenProvider;
    @Mock MenuBuilderService menuBuilderService;
    @Mock LegalDocumentService legalDocumentService;
    @Mock AuditLogService auditLogService;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock CompanyMembershipService companyMembershipService;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuario;
    private Company company;
    private RefreshToken rotado;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);

        usuario = new Usuario();
        usuario.setId(10);
        usuario.setEmail("ana@example.test");
        usuario.setActivo(true);

        Role rol = new Role();
        rol.setId(2);
        rol.setName("ROLE_VETERINARIO");
        rol.setActivo(true);
        rol.setScope(RoleScope.STAFF);
        rol.setPurpose(RolePurpose.CUSTOM);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignacion.setCompany(company);
        usuario.getUsuariosPorRol().add(asignacion);

        rotado = RefreshToken.builder()
                .usuario(usuario)
                .company(company)
                .jti("jti-viejo")
                .familyId(FAMILIA)
                .sessionStartedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        ReflectionTestUtils.setField(service, "absoluteTimeoutSeconds", 86400L);
        ReflectionTestUtils.setField(service, "refreshReuseGraceSeconds", 20L);

        lenient().when(tokenProvider.getRefreshTokenDetails(TOKEN))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-viejo", FAMILIA, "ROLE_VETERINARIO", 2, 0L));
        lenient().when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(rotado));
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setPasswordChanged(true);
        credencial.setCredentialsVersion(0L);
        lenient().when(credencialRepository.findByUsuarioIdAndCompanyId(10, COMPANY_ID)).thenReturn(Optional.of(credencial));
        lenient().when(companyMembershipService.hasActiveMembership(10, COMPANY_ID)).thenReturn(true);
        lenient().when(empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(10, COMPANY_ID)).thenReturn(true);
        lenient().when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyString()))
                .thenReturn("access-nuevo");
        lenient().when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-nuevo");
        lenient().when(tokenProvider.getRefreshTokenDetails("refresh-nuevo"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-nuevo", FAMILIA, null, null, 0L));
        lenient().when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(FAMILIA)).thenReturn(List.of());
    }

    private void rotadoHace(long segundos) {
        Instant momento = AppClock.instantNow().minusSeconds(segundos);
        rotado.setUsedAt(momento);
        rotado.setRevokedAt(momento);
    }

    @Test
    void unTokenRecienRotadoQueVuelveAPresentarseDentroDelMargenRenuevaSinRevocarLaSesion() {
        rotadoHace(3);
        when(refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull(FAMILIA)).thenReturn(true);
        Instant rotadoEn = rotado.getUsedAt();

        AuthResponse respuesta = service.refreshToken(TOKEN);

        assertThat(respuesta.getToken()).isEqualTo("access-nuevo");
        assertThat(respuesta.getRefreshToken()).isEqualTo("refresh-nuevo");
        assertThat(rotado.getUsedAt()).isEqualTo(rotadoEn);
        verify(refreshTokenRepository, never()).saveAll(any());
    }

    @Test
    void pasadoElMargenLaReutilizacionRevocaLaSesion() {
        rotadoHace(60);
        RefreshToken vigente = RefreshToken.builder().familyId(FAMILIA).build();
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(FAMILIA)).thenReturn(List.of(vigente));

        assertThatThrownBy(() -> service.refreshToken(TOKEN)).isInstanceOf(RefreshTokenReuseException.class);

        assertThat(vigente.getRevokedAt()).isNotNull();
    }

    @Test
    void siLaSesionYaSeCerroElTokenRotadoNoLaResucita() {
        rotadoHace(3);
        when(refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull(FAMILIA)).thenReturn(false);

        assertThatThrownBy(() -> service.refreshToken(TOKEN)).isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    void unTokenRevocadoPorCierreDeSesionNoTieneMargen() {
        rotado.setRevokedAt(AppClock.instantNow().minusSeconds(3));

        assertThatThrownBy(() -> service.refreshToken(TOKEN)).isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    void sinMargenConfiguradoSeComportaComoAntes() {
        ReflectionTestUtils.setField(service, "refreshReuseGraceSeconds", 0L);
        rotadoHace(3);

        assertThatThrownBy(() -> service.refreshToken(TOKEN)).isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    void unTokenSinUsarSeRotaComoSiempre() {
        AuthResponse respuesta = service.refreshToken(TOKEN);

        assertThat(respuesta.getRefreshToken()).isEqualTo("refresh-nuevo");
        assertThat(rotado.getUsedAt()).isNotNull();
        assertThat(rotado.getRevokedAt()).isEqualTo(rotado.getUsedAt());
        verify(refreshTokenRepository).save(rotado);
    }
}
