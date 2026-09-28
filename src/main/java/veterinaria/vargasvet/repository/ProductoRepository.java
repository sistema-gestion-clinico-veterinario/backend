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
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCompanyIdAndSku(Integer companyId, String sku);

    boolean existsByCompanyIdAndSku(Integer companyId, String sku);

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

    long countByCompanyIdAndCategoriaId(Integer companyId, Long categoriaId);

    long countByCompanyIdAndUnidadMedidaId(Integer companyId, Long unidadMedidaId);

    @Query("SELECT p.categoria.id, COUNT(p) FROM Producto p " +
            "WHERE p.company.id = :companyId AND p.categoria.id IN :categoriaIds GROUP BY p.categoria.id")
    List<Object[]> countPorCategoriaIds(@Param("companyId") Integer companyId,
                                        @Param("categoriaIds") List<Long> categoriaIds);

    @Query("SELECT p.unidadMedida.id, COUNT(p) FROM Producto p " +
            "WHERE p.company.id = :companyId AND p.unidadMedida.id IN :unidadIds GROUP BY p.unidadMedida.id")
    List<Object[]> countPorUnidadMedidaIds(@Param("companyId") Integer companyId,
                                           @Param("unidadIds") List<Long> unidadIds);

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
