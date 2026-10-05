package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Company;
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

    /** ¿La sesión (familia) sigue abierta? Se consulta en cada petición: usa idx_refresh_tokens_family_active. */
    boolean existsByFamilyIdAndRevokedAtIsNull(String familyId);

    @Query("select distinct r.familyId from RefreshToken r where r.familyId in :familyIds and r.revokedAt is null")
    List<String> findActiveFamilyIds(@Param("familyIds") java.util.Collection<String> familyIds);
    List<RefreshToken> findAllByUsuarioAndRevokedAtIsNull(Usuario usuario);
    List<RefreshToken> findAllByUsuarioAndCompanyAndRevokedAtIsNull(Usuario usuario, Company company);

    /** Bloquea la sesión mientras se reemplaza, para que dos cambios simultáneos no abran dos sesiones nuevas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.familyId = :familyId and r.revokedAt is null")
    List<RefreshToken> findActiveByFamilyIdForUpdate(@Param("familyId") String familyId);


    @Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUsuario(Usuario usuario);
}
