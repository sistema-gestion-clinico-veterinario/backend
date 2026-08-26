package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.RegistroVacuna;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {
    List<RegistroVacuna> findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    Optional<RegistroVacuna> findFirstByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    @Query("SELECT r FROM RegistroVacuna r " +
           "JOIN r.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId AND r.activo = true AND r.fechaProximaDosis IS NOT NULL AND r.fechaProximaDosis BETWEEN :desde AND :hasta " +
           "ORDER BY r.fechaProximaDosis ASC")
    List<RegistroVacuna> findProximasVacunas(@Param("companyId") Integer companyId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
