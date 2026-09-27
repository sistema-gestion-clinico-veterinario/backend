package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Producto;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCompanyIdAndActivoTrue(Integer companyId);

    @Query("SELECT p FROM Producto p WHERE p.company.id = :companyId " +
            "AND (:search IS NULL OR :search = '' " +
            "     OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(p.marca, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(COALESCE(p.codigoBarras, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId) " +
            "AND (:activo IS NULL OR p.activo = :activo)")
    Page<Producto> buscar(@Param("companyId") Integer companyId, @Param("search") String search,
                           @Param("categoriaId") Long categoriaId, @Param("activo") Boolean activo, Pageable pageable);

    @Query("SELECT p.categoria.id, p.categoria.nombre, COUNT(p) FROM Producto p " +
            "WHERE p.company.id = :companyId AND p.activo = true GROUP BY p.categoria.id, p.categoria.nombre")
    List<Object[]> countActivosPorCategoria(@Param("companyId") Integer companyId);

    @Query("SELECT p.sku FROM Producto p WHERE p.company.id = :companyId")
    List<String> findSkusByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT l.producto.id, MIN(l.fechaVencimiento) FROM Lote l " +
            "WHERE l.producto.id IN :productoIds AND l.activo = true GROUP BY l.producto.id")
    List<Object[]> findProximoVencimientoPorProducto(@Param("productoIds") List<Long> productoIds);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") Integer cantidad);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int restaurarStock(@Param("id") Long id, @Param("cantidad") Integer cantidad);
}
