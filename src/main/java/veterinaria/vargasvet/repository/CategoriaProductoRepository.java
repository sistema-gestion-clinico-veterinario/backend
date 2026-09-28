package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;

import java.util.List;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {

    Page<CategoriaProducto> findByCompanyId(Integer companyId, Pageable pageable);

    @Query("SELECT c FROM CategoriaProducto c WHERE c.company.id = :companyId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(c.descripcion, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:activo IS NULL OR c.activo = :activo)")
    Page<CategoriaProducto> buscar(@Param("companyId") Integer companyId,
                                   @Param("search") String search,
                                   @Param("activo") Boolean activo,
                                   Pageable pageable);

    List<CategoriaProducto> findByCompanyIdAndActivoTrue(Integer companyId);
}
