package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.EmpleadoVeterinario;

@Repository
public interface EmpleadoVeterinarioRepository extends JpaRepository<EmpleadoVeterinario, Long> {
}
