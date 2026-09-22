package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionSecurityService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioEmpresaCredencialRepository credencialRepository;

    /** Invalida TODAS las sesiones de la persona, en todas sus empresas - para eventos
     * de seguridad realmente globales (ej. suspensión de cuenta completa). Para un cambio
     * de contraseña o reset, que solo afecta a una empresa, usar
     * invalidateSessionsForCredential en su lugar. */
    @Transactional
    public void invalidateAllSessions(Usuario usuario) {
        List<UsuarioEmpresaCredencial> credenciales = credencialRepository.findAllByUsuarioId(usuario.getId());
        credenciales.forEach(c -> c.setCredentialsVersion(c.getCredentialsVersion() + 1L));
        credencialRepository.saveAll(credenciales);
        revokeRefreshTokens(usuario, AppClock.instantNow());
    }

    /** Invalida solo las sesiones abiertas con la empresa de esta credencial específica -
     * un cambio o reset de contraseña en la Empresa A no debe desloguear a la persona de
     * la Empresa B. Para SuperAdmin (credencial sin empresa) no hay otras sesiones que
     * distinguir, así que se revocan todas igual. */
    @Transactional
    public void invalidateSessionsForCredential(UsuarioEmpresaCredencial credencial) {
        credencial.setCredentialsVersion(credencial.getCredentialsVersion() + 1L);
        credencialRepository.save(credencial);
        if (credencial.getCompany() == null) {
            revokeRefreshTokens(credencial.getUsuario(), AppClock.instantNow());
        } else {
            invalidateSessionsForCompany(credencial.getUsuario(), credencial.getCompany());
        }
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
        // Un parametro Company null en una query derivada de Spring Data genera
        // "company_id = NULL" en SQL, que nunca es verdadero para ninguna fila - sin este
        // caso especial, un SuperAdmin (sin empresa) no revocaria ningun refresh token.
        if (company == null) {
            revokeRefreshTokens(usuario, AppClock.instantNow());
            return;
        }
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByUsuarioAndCompanyAndRevokedAtIsNull(usuario, company);
        Instant revokedAt = AppClock.instantNow();
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }
}
