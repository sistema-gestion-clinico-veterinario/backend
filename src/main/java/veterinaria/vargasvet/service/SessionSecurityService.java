package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionSecurityService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void invalidateAllSessions(Usuario usuario) {
        usuario.setCredentialsVersion(usuario.getCredentialsVersion() + 1L);
        usuarioRepository.save(usuario);
        revokeRefreshTokens(usuario, AppClock.instantNow());
    }

    @Transactional
    public void revokeRefreshTokens(Usuario usuario, Instant revokedAt) {
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUsuarioAndRevokedAtIsNull(usuario);
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }
}
