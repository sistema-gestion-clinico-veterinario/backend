package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionSecurityServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @InjectMocks SessionSecurityService service;

    @Test
    void invalidarSesionesIncrementaVersionYRevocaRefreshActivos() {
        Usuario usuario = new Usuario();
        usuario.setCredentialsVersion(3L);
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findAllByUsuarioAndRevokedAtIsNull(usuario))
                .thenReturn(List.of(token));

        service.invalidateAllSessions(usuario);

        assertEquals(4L, usuario.getCredentialsVersion());
        assertNotNull(token.getRevokedAt());
        verify(usuarioRepository).save(usuario);
        verify(refreshTokenRepository).saveAll(List.of(token));
    }
}
