package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;

import java.util.List;

@Repository
public interface HorarioEmpleadoRepository extends JpaRepository<HorarioEmpleado, Long> {
    List<HorarioEmpleado> findByEmpleadoId(Long empleadoId);
    void deleteByEmpleadoId(Long empleadoId);
}
