package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.CategoriaProducto;

import java.util.List;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {

    Page<CategoriaProducto> findByCompanyId(Integer companyId, Pageable pageable);

    List<CategoriaProducto> findByCompanyIdAndActivoTrue(Integer companyId);
}
