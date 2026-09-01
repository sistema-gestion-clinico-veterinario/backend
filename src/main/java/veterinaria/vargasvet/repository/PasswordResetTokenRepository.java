package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM PasswordResetToken token JOIN FETCH token.usuario WHERE token.token = :tokenHash")
    Optional<PasswordResetToken> findByTokenForUpdate(@Param("tokenHash") String tokenHash);
    void deleteByUsuario(Usuario usuario);
}
