package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {
    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);
    boolean existsByNumeroHc(String numeroHc);
}
