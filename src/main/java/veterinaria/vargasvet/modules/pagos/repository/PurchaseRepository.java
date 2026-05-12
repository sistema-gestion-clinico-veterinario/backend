package veterinaria.vargasvet.modules.pagos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.pagos.domain.entity.Purchase;
import veterinaria.vargasvet.modules.pagos.domain.enums.TipoPurchase;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    boolean existsByCitaIdAndTipoPurchase(Long citaId, TipoPurchase tipoPurchase);
    Optional<Purchase> findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(Long citaId, TipoPurchase tipoPurchase);
}
