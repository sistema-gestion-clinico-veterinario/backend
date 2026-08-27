package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.RegistroVacuna;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {
    interface UltimaAplicacionPorMascota {
        Long getMascotaId();
        LocalDate getFecha();
    }

    List<RegistroVacuna> findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    Optional<RegistroVacuna> findFirstByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    @Query("SELECT h.mascota.id AS mascotaId, MAX(r.fechaAplicacion) AS fecha " +
           "FROM RegistroVacuna r JOIN r.historiaClinica h " +
           "WHERE h.mascota.id IN :mascotaIds GROUP BY h.mascota.id")
    List<UltimaAplicacionPorMascota> findUltimasAplicacionesPorMascota(
            @Param("mascotaIds") List<Long> mascotaIds);

    @Query("SELECT r FROM RegistroVacuna r " +
           "JOIN r.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId AND r.activo = true AND r.fechaProximaDosis IS NOT NULL AND r.fechaProximaDosis BETWEEN :desde AND :hasta " +
           "ORDER BY r.fechaProximaDosis ASC")
    List<RegistroVacuna> findProximasVacunas(@Param("companyId") Integer companyId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT r FROM RegistroVacuna r " +
           "JOIN FETCH r.historiaClinica h JOIN FETCH h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId AND r.activo = true AND r.fechaProximaDosis IS NOT NULL " +
           "AND r.fechaProximaDosis BETWEEN :desde AND :hasta ORDER BY r.fechaProximaDosis ASC")
    List<RegistroVacuna> findProximasVacunas(@Param("companyId") Integer companyId,
                                              @Param("desde") LocalDate desde,
                                              @Param("hasta") LocalDate hasta,
                                              Pageable pageable);
}
