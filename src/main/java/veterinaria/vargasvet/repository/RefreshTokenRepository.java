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

import java.time.Instant;
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

    interface ActividadDeSesion {
        long getAbiertas();
        Instant getUltimaActividad();
    }

    /** Estado de la sesión en una sola lectura por petición: cuántos tokens siguen abiertos y cuándo fue su última actividad. */
    @Query("select count(r) as abiertas, max(r.lastActivityAt) as ultimaActividad "
            + "from RefreshToken r where r.familyId = :familyId and r.revokedAt is null")
    ActividadDeSesion resumenDeActividad(@Param("familyId") String familyId);

    /** Última actividad de toda la sesión, incluidos los tokens ya rotados. */
    @Query("select max(r.lastActivityAt) from RefreshToken r where r.familyId = :familyId")
    Instant ultimaActividadDeLaSesion(@Param("familyId") String familyId);

    interface ActividadDePersona {
        Integer getUsuarioId();
        Instant getUltimaActividad();
    }

    @Query("select r.usuario.id as usuarioId, max(r.lastActivityAt) as ultimaActividad "
            + "from RefreshToken r where r.usuario.id in :usuarioIds and r.company.id = :companyId "
            + "group by r.usuario.id")
    List<ActividadDePersona> ultimaActividadPorPersona(
            @Param("usuarioIds") java.util.Collection<Integer> usuarioIds, @Param("companyId") Integer companyId);

    @Modifying
    @Query("update RefreshToken r set r.lastActivityAt = :ahora where r.familyId = :familyId and r.revokedAt is null")
    int registrarActividad(@Param("familyId") String familyId, @Param("ahora") Instant ahora);

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
