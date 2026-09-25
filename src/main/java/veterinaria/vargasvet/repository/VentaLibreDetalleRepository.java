package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;

import java.util.List;

@Repository
public interface VentaLibreDetalleRepository extends JpaRepository<VentaLibreDetalle, Long> {

    List<VentaLibreDetalle> findByPurchaseId(Long purchaseId);
}
