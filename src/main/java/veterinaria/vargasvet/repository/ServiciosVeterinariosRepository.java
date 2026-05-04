package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;

@Repository
public interface ServiciosVeterinariosRepository extends JpaRepository<ServiciosVeterinarios, Long> {
}
