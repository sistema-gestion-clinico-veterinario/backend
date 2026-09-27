package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.UnidadMedida;

import java.util.List;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    Page<UnidadMedida> findByCompanyId(Integer companyId, Pageable pageable);

    List<UnidadMedida> findByCompanyIdAndActivoTrue(Integer companyId);
}
