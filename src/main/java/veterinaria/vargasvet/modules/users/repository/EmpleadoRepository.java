package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.Empleado;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByUserId(Integer userId);
    List<Empleado> findByUserCompanyId(Integer companyId);
    boolean existsByNumeroDocumentoIdentidad(String numeroDocumentoIdentidad);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    long countByUserCompanyId(Integer companyId);
    default long countByCompanyId(Integer companyId) { return countByUserCompanyId(companyId); }
}
