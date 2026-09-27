package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.AjusteStock;

@Repository
public interface AjusteStockRepository extends JpaRepository<AjusteStock, Long> {

    Page<AjusteStock> findByProductoIdOrderByCreatedAtDesc(Long productoId, Pageable pageable);

    Page<AjusteStock> findByCompanyIdOrderByCreatedAtDesc(Integer companyId, Pageable pageable);
}
