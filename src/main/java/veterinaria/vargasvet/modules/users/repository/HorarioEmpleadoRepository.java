package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.HorarioEmpleado;

import java.util.List;

@Repository
public interface HorarioEmpleadoRepository extends JpaRepository<HorarioEmpleado, Long> {
    List<HorarioEmpleado> findByEmpleadoId(Long empleadoId);
}
