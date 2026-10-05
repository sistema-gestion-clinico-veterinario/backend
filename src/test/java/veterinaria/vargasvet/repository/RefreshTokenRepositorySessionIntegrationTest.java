package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La consulta con bloqueo que usa el cambio de rol debe devolver solo los tokens vigentes de la
 * sesión indicada: ni los ya revocados ni los de otras sesiones del mismo usuario.
 */
@DataJpaTest
@Transactional
class RefreshTokenRepositorySessionIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RefreshTokenRepository repository;

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("u-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setUsername("u-" + UUID.randomUUID());
        usuario.setNombre("Ana");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private RefreshToken token(Usuario usuario, String familyId, Instant revokedAt) {
        return repository.saveAndFlush(RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(UUID.randomUUID().toString())
                .jti(UUID.randomUUID().toString())
                .familyId(familyId)
                .revokedAt(revokedAt)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .sessionStartedAt(Instant.now())
                .build());
    }

    @Test
    void devuelveSoloElTokenVigenteDeLaSesionPedida() {
        Usuario usuario = usuario();
        RefreshToken vigente = token(usuario, "sesion-a", null);
        token(usuario, "sesion-a", Instant.now());
        token(usuario, "sesion-b", null);

        List<RefreshToken> resultado = repository.findActiveByFamilyIdForUpdate("sesion-a");

        assertThat(resultado).extracting(RefreshToken::getId).containsExactly(vigente.getId());
    }

    @Test
    void unaSesionYaRevocadaNoDevuelveNada() {
        Usuario usuario = usuario();
        token(usuario, "sesion-cerrada", Instant.now());

        assertThat(repository.findActiveByFamilyIdForUpdate("sesion-cerrada")).isEmpty();
        assertThat(repository.findActiveByFamilyIdForUpdate("no-existe")).isEmpty();
    }
}
