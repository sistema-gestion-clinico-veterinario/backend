package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;

import java.util.List;

@Repository
public interface VentaLibreDetalleRepository extends JpaRepository<VentaLibreDetalle, Long> {

    List<VentaLibreDetalle> findByPurchaseId(Long purchaseId);

    @Query("SELECT d FROM VentaLibreDetalle d JOIN FETCH d.producto p LEFT JOIN FETCH p.categoria " +
           "WHERE d.purchase.id IN :purchaseIds")
    List<VentaLibreDetalle> findForReportByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);
}
