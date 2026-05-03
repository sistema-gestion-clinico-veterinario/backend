package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;

import java.util.Optional;

@Repository
public interface TipoEmpleadoRepository extends JpaRepository<TipoEmpleado, Long> {
    Optional<TipoEmpleado> findByNombre(String nombre);
}
