package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.TipoEmpleado;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoEmpleadoRepository extends JpaRepository<TipoEmpleado, Long> {
    List<TipoEmpleado> findByCompanyId(Integer companyId);
    List<TipoEmpleado> findByCompanyIdAndEstadoTrue(Integer companyId);
    Optional<TipoEmpleado> findByNombreAndCompanyId(String nombre, Integer companyId);
    Optional<TipoEmpleado> findByNombre(String nombre);
}
