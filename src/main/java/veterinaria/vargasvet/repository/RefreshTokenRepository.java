package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r join fetch r.usuario where r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findAllByFamilyIdAndRevokedAtIsNull(String familyId);
    List<RefreshToken> findAllByUsuarioAndRevokedAtIsNull(Usuario usuario);
    Optional<RefreshToken> findFirstByUsuarioOrderByExpiryDateDesc(Usuario usuario);
    
    @Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUsuario(Usuario usuario);
}
