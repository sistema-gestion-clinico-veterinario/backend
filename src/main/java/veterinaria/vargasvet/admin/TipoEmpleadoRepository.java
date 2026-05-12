package veterinaria.vargasvet.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.admin.TipoEmpleado;

import java.util.Optional;

@Repository
public interface TipoEmpleadoRepository extends JpaRepository<TipoEmpleado, Long> {
    Optional<TipoEmpleado> findByNombre(String nombre);
    java.util.List<TipoEmpleado> findByCompanyId(Integer companyId);
    Optional<TipoEmpleado> findByNombreAndCompanyId(String nombre, Integer companyId);
}
