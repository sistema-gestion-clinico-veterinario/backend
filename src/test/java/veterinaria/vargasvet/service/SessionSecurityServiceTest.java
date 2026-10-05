package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.RealtimeSubscriptionGuard;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionSecurityServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UsuarioEmpresaCredencialRepository credencialRepository;
    @Mock RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    @InjectMocks SessionSecurityService service;

    private RefreshToken token(String familyId) {
        RefreshToken token = new RefreshToken();
        token.setFamilyId(familyId);
        return token;
    }

    @Test
    void alRevocarSesionesDeUnaEmpresaSeCierranDeInmediatoSusConexionesEnTiempoReal() {
        Usuario usuario = new Usuario();
        Company company = new Company();
        company.setId(5);
        when(refreshTokenRepository.findAllByUsuarioAndCompanyAndRevokedAtIsNull(usuario, company))
                .thenReturn(List.of(token("sesion-a"), token("sesion-a"), token("sesion-b"), token(null)));

        service.invalidateSessionsForCompany(usuario, company);

        verify(realtimeSubscriptionGuard).closeConnectionsOfSession("sesion-a");
        verify(realtimeSubscriptionGuard).closeConnectionsOfSession("sesion-b");
        verifyNoMoreInteractions(realtimeSubscriptionGuard);
    }

    @Test
    void alInvalidarTodasLasSesionesTambienSeCierranSusConexionesEnTiempoReal() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of());
        when(refreshTokenRepository.findAllByUsuarioAndRevokedAtIsNull(usuario))
                .thenReturn(List.of(token("sesion-a"), token("sesion-c")));

        service.invalidateAllSessions(usuario);

        verify(realtimeSubscriptionGuard).closeConnectionsOfSession("sesion-a");
        verify(realtimeSubscriptionGuard).closeConnectionsOfSession("sesion-c");
    }

    @Test
    void sinSesionesActivasNoHayConexionesQueCerrar() {
        Usuario usuario = new Usuario();
        Company company = new Company();
        when(refreshTokenRepository.findAllByUsuarioAndCompanyAndRevokedAtIsNull(usuario, company))
                .thenReturn(List.of());

        service.invalidateSessionsForCompany(usuario, company);

        verifyNoInteractions(realtimeSubscriptionGuard);
    }

    @Test
    void invalidarTodasLasSesionesIncrementaVersionDeCadaCredencialYRevocaRefreshActivos() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        UsuarioEmpresaCredencial credencialA = new UsuarioEmpresaCredencial();
        credencialA.setCredentialsVersion(3L);
        RefreshToken token = new RefreshToken();
        when(credencialRepository.findAllByUsuarioId(10)).thenReturn(List.of(credencialA));
        when(refreshTokenRepository.findAllByUsuarioAndRevokedAtIsNull(usuario))
                .thenReturn(List.of(token));

        service.invalidateAllSessions(usuario);

        assertEquals(4L, credencialA.getCredentialsVersion());
        assertNotNull(token.getRevokedAt());
        verify(credencialRepository).saveAll(List.of(credencialA));
        verify(refreshTokenRepository).saveAll(List.of(token));
    }

    @Test
    void invalidarSesionesDeUnaCredencialSoloRevocaLasDeEsaEmpresa() {
        Usuario usuario = new Usuario();
        usuario.setId(10);
        Company company = new Company();
        company.setId(5);
        UsuarioEmpresaCredencial credencial = new UsuarioEmpresaCredencial();
        credencial.setUsuario(usuario);
        credencial.setCompany(company);
        credencial.setCredentialsVersion(1L);
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findAllByUsuarioAndCompanyAndRevokedAtIsNull(usuario, company))
                .thenReturn(List.of(token));

        service.invalidateSessionsForCredential(credencial);

        assertEquals(2L, credencial.getCredentialsVersion());
        assertNotNull(token.getRevokedAt());
        verify(credencialRepository).save(credencial);
        verify(refreshTokenRepository, org.mockito.Mockito.never()).findAllByUsuarioAndRevokedAtIsNull(any());
    }
}
