package veterinaria.vargasvet.servicios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.servicios.ServiciosVeterinarios;

import java.util.List;

@Repository
public interface ServiciosVeterinariosRepository extends JpaRepository<ServiciosVeterinarios, Long> {

    Page<ServiciosVeterinarios> findByCompanyId(Integer companyId, Pageable pageable);

    List<ServiciosVeterinarios> findByCompanyIdAndDisponibleTrueAndActivoTrue(Integer companyId);
}
