package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(Long citaId, TipoPurchase tipoPurchase);

    boolean existsByCitaIdAndTipoPurchase(Long citaId, TipoPurchase tipoPurchase);

    Page<Purchase> findAllByTipoPurchaseOrderByCreatedAtDesc(TipoPurchase tipoPurchase, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.cita.mascota.apoderado.user.id = :userId AND p.tipoPurchase = :tipo ORDER BY p.createdAt DESC")
    Page<Purchase> findByApoderadoUserId(@Param("userId") Integer userId, @Param("tipo") TipoPurchase tipo, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.cita.mascota.apoderado.user.company.id = :companyId AND p.tipoPurchase = :tipo ORDER BY p.createdAt DESC")
    Page<Purchase> findByCompanyId(@Param("companyId") Integer companyId, @Param("tipo") TipoPurchase tipo, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.user.company.id = :companyId ORDER BY p.createdAt DESC")
    Page<Purchase> findByUserCompanyId(@Param("companyId") Integer companyId, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.user.company.id = :companyId AND p.cita IS NULL ORDER BY p.createdAt DESC")
    Page<Purchase> findByUserCompanyIdNonCita(@Param("companyId") Integer companyId, Pageable pageable);
}
