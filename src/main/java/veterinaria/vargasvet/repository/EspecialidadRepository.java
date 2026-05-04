package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Especialidad;

import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    Optional<Especialidad> findByNombre(String nombre);
    java.util.List<Especialidad> findByCompanyId(Integer companyId);
    Optional<Especialidad> findByNombreAndCompanyId(String nombre, Integer companyId);
}
