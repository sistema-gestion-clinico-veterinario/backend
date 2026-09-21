package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
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

    /** Revoca solo las sesiones (refresh tokens) que el usuario estableció con esta
     * empresa. No toca usuario.credentialsVersion (eso invalidaría también los access
     * tokens de otras empresas donde el mismo usuario sigue activo) ni las sesiones de
     * otras empresas - uso pensado para suspender la relación de un apoderado con una
     * sola empresa sin desloguearlo de las demás. */
    @Transactional
    public void invalidateSessionsForCompany(Usuario usuario, Company company) {
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUsuarioAndCompanyAndRevokedAtIsNull(usuario, company);
        Instant revokedAt = AppClock.instantNow();
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }
}
