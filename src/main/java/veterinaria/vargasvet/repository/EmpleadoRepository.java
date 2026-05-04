package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Empleado;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    Optional<Empleado> findByUserId(Integer userId);
}
