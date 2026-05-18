package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUsuario(Usuario usuario);
}
