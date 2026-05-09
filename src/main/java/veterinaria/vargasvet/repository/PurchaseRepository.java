package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(Long citaId, TipoPurchase tipoPurchase);

    boolean existsByCitaIdAndTipoPurchase(Long citaId, TipoPurchase tipoPurchase);
}
