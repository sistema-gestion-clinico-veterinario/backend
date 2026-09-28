package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.UnidadMedida;

import java.util.List;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    Page<UnidadMedida> findByCompanyId(Integer companyId, Pageable pageable);

    @Query("SELECT u FROM UnidadMedida u WHERE u.company.id = :companyId " +
            "AND (:search IS NULL OR :search = '' OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(u.descripcion, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:activo IS NULL OR u.activo = :activo)")
    Page<UnidadMedida> buscar(@Param("companyId") Integer companyId,
                             @Param("search") String search,
                             @Param("activo") Boolean activo,
                             Pageable pageable);

    List<UnidadMedida> findByCompanyIdAndActivoTrue(Integer companyId);
}
