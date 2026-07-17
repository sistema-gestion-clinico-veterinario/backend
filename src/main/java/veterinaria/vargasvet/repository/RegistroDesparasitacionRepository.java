package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.RegistroDesparasitacion;

import java.util.List;

public interface RegistroDesparasitacionRepository extends JpaRepository<RegistroDesparasitacion, Long> {
    List<RegistroDesparasitacion> findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);
}
