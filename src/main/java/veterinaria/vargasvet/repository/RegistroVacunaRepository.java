package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.RegistroVacuna;

import java.util.List;

public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {
    List<RegistroVacuna> findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);
}
