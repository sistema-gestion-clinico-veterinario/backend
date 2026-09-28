package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.MarcaProducto;

import java.util.List;

@Repository
public interface MarcaProductoRepository extends JpaRepository<MarcaProducto, Long> {

    List<MarcaProducto> findByCompanyIdAndActivoTrueOrderByNombreAsc(Integer companyId);

    boolean existsByCompanyIdAndNombreIgnoreCase(Integer companyId, String nombre);

    boolean existsByCompanyIdAndNombreIgnoreCaseAndIdNot(Integer companyId, String nombre, Long id);

    @Query("SELECT m FROM MarcaProducto m WHERE m.company.id = :companyId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(m.descripcion, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:activo IS NULL OR m.activo = :activo)")
    Page<MarcaProducto> buscar(@Param("companyId") Integer companyId,
                               @Param("search") String search,
                               @Param("activo") Boolean activo,
                               Pageable pageable);
}
