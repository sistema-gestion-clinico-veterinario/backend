package veterinaria.vargasvet.pagos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.pagos.Purchase;
import veterinaria.vargasvet.shared.TipoPurchase;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(Long citaId, TipoPurchase tipoPurchase);

    boolean existsByCitaIdAndTipoPurchase(Long citaId, TipoPurchase tipoPurchase);
}
