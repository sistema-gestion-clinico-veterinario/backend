package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.service.CompanyMembershipService;
import veterinaria.vargasvet.service.LegalDocumentService;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cubre login(): aislamiento total entre empresas - ya no existe login "global" sin
 * slug (se rechaza siempre), y el username/correo puede repetirse entre empresas sin
 * relacion entre si, asi que el login desambigua quedandose con el candidato que tiene
 * membresia activa en la empresa del slug.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplLoginTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CompanyRepository companyRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock TokenProvider tokenProvider;
    @Mock SharedRateLimitService sharedRateLimitService;
    @Mock AccountLockoutService accountLockoutService;
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

    private LoginDTO loginConSlug(String slug) {
        LoginDTO dto = new LoginDTO();
        dto.setSlug(slug);
        dto.setUsername("ana.qa");
        dto.setPassword("Password-123");
        return dto;
    }

    @Test
    void rechazaLoginSinSlug() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("ana.qa");
        dto.setPassword("Password-123");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(companyRepository, usuarioRepository);
    }

    @Test
    void rechazaLoginCuandoElSlugNoCorrespondeAUnaEmpresa() {
        when(companyRepository.findBySlug("empresa-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginConSlug("empresa-inexistente")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void rechazaLoginCuandoNingunCandidatoTieneMembresiaActivaEnEsaEmpresa() {
        Company company = new Company();
        company.setId(7);
        company.setSlug("vargas-vet");
        Usuario usuario = usuarioValido();

        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(usuario));
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(false);
        when(usuarioRepository.findAllByEmailIgnoreCase("ana.qa")).thenReturn(List.of());

        assertThatThrownBy(() -> service.login(loginConSlug("vargas-vet")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verify(credencialRepository, never()).findByUsuarioIdAndCompanyId(any(), any());
    }

    @Test
    void desambiguaEntreDosCandidatosConElMismoUsernameEnEmpresasDistintasPorMembresiaActiva() {
        // El username ya no es unico globalmente - puede existir "ana.qa" en dos
        // empresas sin relacion entre si. El login se queda con el que SI tiene
        // membresia activa en la empresa del slug, nunca con el primero que aparezca.
        Company company = new Company();
        company.setId(7);
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        company.setActivo(true);

        Usuario deOtraEmpresa = usuarioValido();
        deOtraEmpresa.setId(11);
        Usuario elCorrecto = usuarioValido();
        elCorrecto.setId(10);

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = credencialValida();

        when(companyRepository.findBySlug("vargas-vet")).thenReturn(Optional.of(company));
        when(usuarioRepository.findAllByUsernameIgnoreCase("ana.qa")).thenReturn(List.of(deOtraEmpresa, elCorrecto));
        when(companyMembershipService.hasActiveMembership(11, 7)).thenReturn(false);
        when(companyMembershipService.hasActiveMembership(10, 7)).thenReturn(true);
        when(credencialRepository.findByUsuarioIdAndCompanyId(10, 7)).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches("Password-123", "hash-almacenado")).thenReturn(true);
        when(tokenProvider.createToken(any(), any(), any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn("access-token");
        when(tokenProvider.createRefreshToken(anyString(), any(), any(), anyString(), anyLong()))
                .thenReturn("refresh-token");
        when(tokenProvider.getRefreshTokenDetails("refresh-token"))
                .thenReturn(new TokenProvider.RefreshTokenDetails("ana@example.test", "jti-1", "family-1", null, null, 0L));

        AuthResponse response = service.login(loginConSlug("vargas-vet"));

        assertThat(response.getCompanyId()).isEqualTo(7);
        assertThat(response.getCompanyName()).isEqualTo("Vargas Vet");
    }
}
