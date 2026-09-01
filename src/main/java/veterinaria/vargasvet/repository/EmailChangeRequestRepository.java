package veterinaria.vargasvet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;

public interface EmailChangeRequestRepository extends JpaRepository<EmailChangeRequest, Long> {

    void deleteByUsuario(Usuario usuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM EmailChangeRequest request " +
            "JOIN FETCH request.usuario WHERE request.oldEmailTokenHash = :tokenHash")
    Optional<EmailChangeRequest> findByOldTokenForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM EmailChangeRequest request " +
            "JOIN FETCH request.usuario WHERE request.newEmailTokenHash = :tokenHash")
    Optional<EmailChangeRequest> findByNewTokenForUpdate(@Param("tokenHash") String tokenHash);
}
