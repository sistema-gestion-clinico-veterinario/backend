package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.VentaLibreDetalle;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaLibreDetalleRepository extends JpaRepository<VentaLibreDetalle, Long> {

    String PRODUCTOS_QUE_COINCIDEN = "(CAST(:productoId AS string) IS NULL OR pr.id = :productoId) " +
            "AND (CAST(:categoriaId AS string) IS NULL OR pr.categoria.id = :categoriaId) " +
            "AND (CAST(:marcaId AS string) IS NULL OR pr.marcaProducto.id = :marcaId) ";

    interface ProductoVendidoEnElPeriodo {
        Long getProductoId();
        String getSku();
        String getNombre();
        Long getCantidad();
        BigDecimal getMonto();
    }

    List<VentaLibreDetalle> findByPurchaseId(Long purchaseId);

    @Query("SELECT p.metodoPago AS metodoPago, CAST(p.createdAt AS date) AS dia, COUNT(DISTINCT p.id) AS ventas, " +
           "COALESCE(SUM(d.subtotal), 0) AS monto " +
           "FROM VentaLibreDetalle d JOIN d.purchase p JOIN d.producto pr WHERE " +
           PurchaseRepository.VENTAS_DE_TIENDA_EN_EL_PERIODO + "AND " + PRODUCTOS_QUE_COINCIDEN +
           "GROUP BY p.metodoPago, CAST(p.createdAt AS date)")
    List<PurchaseRepository.VentaAgrupada> resumirVentasConProductos(
            @Param("companyId") Integer companyId,
            @Param("tipo") TipoPurchase tipo,
            @Param("estado") PaymentStatus estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("registradoPorUsuarioId") Integer registradoPorUsuarioId,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("productoId") Long productoId,
            @Param("categoriaId") Long categoriaId,
            @Param("marcaId") Long marcaId);

    @Query("SELECT pr.id AS productoId, pr.sku AS sku, pr.nombre AS nombre, SUM(d.cantidad) AS cantidad, " +
           "COALESCE(SUM(d.subtotal), 0) AS monto " +
           "FROM VentaLibreDetalle d JOIN d.purchase p JOIN d.producto pr WHERE " +
           PurchaseRepository.VENTAS_DE_TIENDA_EN_EL_PERIODO + "AND " + PRODUCTOS_QUE_COINCIDEN +
           "GROUP BY pr.id, pr.sku, pr.nombre")
    List<ProductoVendidoEnElPeriodo> resumirProductosVendidos(
            @Param("companyId") Integer companyId,
            @Param("tipo") TipoPurchase tipo,
            @Param("estado") PaymentStatus estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("registradoPorUsuarioId") Integer registradoPorUsuarioId,
            @Param("metodoPago") MetodoPago metodoPago,
            @Param("productoId") Long productoId,
            @Param("categoriaId") Long categoriaId,
            @Param("marcaId") Long marcaId);
}
