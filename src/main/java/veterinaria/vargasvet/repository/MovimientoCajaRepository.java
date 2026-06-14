package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.MovimientoCaja;
import veterinaria.vargasvet.domain.enums.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    Page<MovimientoCaja> findByCompanyIdOrderByFechaDesc(Integer companyId, Pageable pageable);

    Page<MovimientoCaja> findByCompanyIdAndFechaBetweenOrderByFechaDesc(
            Integer companyId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m " +
           "WHERE m.companyId = :cId AND m.tipo = :tipo AND m.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumByTipo(@Param("cId") Integer companyId,
                         @Param("tipo") TipoMovimiento tipo,
                         @Param("desde") LocalDateTime desde,
                         @Param("hasta") LocalDateTime hasta);

    boolean existsByCitaIdAndTipo(Long citaId, TipoMovimiento tipo);
}
