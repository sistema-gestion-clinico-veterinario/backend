package veterinaria.vargasvet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.CierreCuenta;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CierreCuentaRepository extends JpaRepository<CierreCuenta, Long> {

    boolean existsByUsuarioIdAndCompanyIdAndEstado(Integer usuarioId, Integer companyId, EstadoCierreCuenta estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CierreCuenta c WHERE c.tokenHash = :tokenHash")
    Optional<CierreCuenta> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<CierreCuenta> findTop100ByEstadoAndVenceAtBeforeOrderByVenceAtAsc(EstadoCierreCuenta estado, LocalDateTime ahora);
}
