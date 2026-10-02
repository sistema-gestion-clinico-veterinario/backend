package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Lote;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import jakarta.persistence.LockModeType;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    Page<Lote> findByProductoId(Long productoId, Pageable pageable);

    @Query("SELECT l FROM Lote l WHERE l.company.id = :companyId " +
            "AND (:search IS NULL OR :search = '' " +
            "     OR LOWER(l.numeroLote) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(l.producto.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(l.producto.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lote> buscar(@Param("companyId") Integer companyId, @Param("search") String search, Pageable pageable);

    List<Lote> findByProductoIdAndActivoTrueOrderByFechaVencimientoAsc(Long productoId);

    long countByProductoId(Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.activo = true " +
            "AND l.cantidad > 0 AND l.fechaVencimiento >= :hoy " +
            "ORDER BY l.fechaVencimiento ASC, l.fechaIngreso ASC, l.id ASC")
    List<Lote> findDisponiblesFefoForUpdate(@Param("productoId") Long productoId,
                                             @Param("hoy") LocalDate hoy);

    @Query("SELECT l.producto.id, SUM(l.cantidad) FROM Lote l " +
            "WHERE l.producto.id IN :productoIds AND l.activo = true " +
            "AND l.cantidad > 0 AND l.fechaVencimiento >= :hoy GROUP BY l.producto.id")
    List<Object[]> sumarDisponiblesPorProducto(@Param("productoIds") Collection<Long> productoIds,
                                                @Param("hoy") LocalDate hoy);

    boolean existsByProductoIdAndNumeroLoteIgnoreCase(Long productoId, String numeroLote);

    boolean existsByProductoIdAndNumeroLoteIgnoreCaseAndIdNot(Long productoId, String numeroLote, Long id);

    List<Lote> findByCompanyIdAndActivoTrueAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
            Integer companyId, LocalDate desde, LocalDate hasta);

    List<Lote> findByCompanyIdAndActivoTrueAndFechaVencimientoBeforeOrderByFechaVencimientoAsc(
            Integer companyId, LocalDate hoy);
}
