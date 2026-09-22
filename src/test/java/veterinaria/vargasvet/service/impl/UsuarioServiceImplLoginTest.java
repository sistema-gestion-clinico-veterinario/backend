package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cubre login() sin slug (la pantalla "global" sin marca de ninguna empresa
 * en particular): solo debe permitirse si el username tiene EXACTAMENTE una
 * empresa activa; con cero o varias se rechaza con el mismo mensaje generico
 * de credenciales invalidas, nunca revelando cuantas o cuales tiene. Antes de
 * este cambio no habia ninguna cobertura de login() en absoluto.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplLoginTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock TokenProvider tokenProvider;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock AuthenticationAuditService authenticationAuditService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock MenuBuilderService menuBuilderService;
    @Mock LegalDocumentService legalDocumentService;
    @Mock AuditLogService auditLogService;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ApoderadoRepository apoderadoRepository;
    @Mock veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository credencialRepository;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuarioValido() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        usuario.setUsername("ana.qa");
        usuario.setEmail("ana@example.test");
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuario;
    }

    private veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencialValida() {
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
        credencial.setPassword("hash-almacenado");
        credencial.setPasswordChanged(true);
        credencial.setCredentialsVersion(0L);
        return credencial;
    }

    private LoginDTO loginSinSlug() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ana.qa");
        dto.setPassword("Password-123");
        return dto;
    }

    @Test
    void rechazaLoginGlobalCuandoElUsuarioNoTieneNingunaEmpresaActiva() {
        Usuario usuario = usuarioValido();
        when(usuarioRepository.findByUsername("ana.qa")).thenReturn(Optional.of(usuario));
        when(companyMembershipService.getActiveCompanyIds(usuario)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.login(loginSinSlug()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(companyRepository, never()).findById(any());
    }

    @Test
    void rechazaLoginGlobalCuandoElUsuarioTieneVariasEmpresasActivas() {
        Usuario usuario = usuarioValido();
        when(usuarioRepository.findByUsername("ana.qa")).thenReturn(Optional.of(usuario));
        when(companyMembershipService.getActiveCompanyIds(usuario)).thenReturn(Set.of(1, 2));

        assertThatThrownBy(() -> service.login(loginSinSlug()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(companyRepository, never()).findById(any());
    }

    @Test
    void permiteLoginGlobalCuandoElUsuarioTieneExactamenteUnaEmpresaActiva() {
        Usuario usuario = usuarioValido();
        Company company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();

        when(usuarioRepository.findByUsername("ana.qa")).thenReturn(Optional.of(usuario));
        when(companyMembershipService.getActiveCompanyIds(usuario)).thenReturn(Set.of(7));
        when(companyRepository.findById(7)).thenReturn(Optional.of(company));
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-1", "family-1", null, null, 0L));

        AuthResponse response = service.login(loginSinSlug());

        assertThat(response.getCompanyId()).isEqualTo(7);
        assertThat(response.getCompanyName()).isEqualTo("Vargas Vet");
        verify(companyRepository).findById(7);
    }
}
