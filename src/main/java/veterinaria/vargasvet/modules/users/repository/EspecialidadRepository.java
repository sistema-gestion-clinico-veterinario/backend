package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.Especialidad;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findByCompanyId(Integer companyId);
    Optional<Especialidad> findByNombreAndCompanyId(String nombre, Integer companyId);
    Optional<Especialidad> findByNombre(String nombre);
}
