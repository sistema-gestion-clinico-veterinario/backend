package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.RegistroDesparasitacion;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface RegistroDesparasitacionRepository extends JpaRepository<RegistroDesparasitacion, Long> {
    interface UltimaAplicacionPorMascota {
        Long getMascotaId();
        LocalDate getFecha();
    }

    List<RegistroDesparasitacion> findByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    Optional<RegistroDesparasitacion> findFirstByHistoriaClinicaMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    @Query("SELECT h.mascota.id AS mascotaId, MAX(r.fechaAplicacion) AS fecha " +
           "FROM RegistroDesparasitacion r JOIN r.historiaClinica h " +
           "WHERE h.mascota.id IN :mascotaIds GROUP BY h.mascota.id")
    List<UltimaAplicacionPorMascota> findUltimasAplicacionesPorMascota(
            @Param("mascotaIds") List<Long> mascotaIds);

    @Query("SELECT r FROM RegistroDesparasitacion r " +
           "JOIN r.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId AND r.activo = true AND r.fechaProximaAplicacion IS NOT NULL AND r.fechaProximaAplicacion BETWEEN :desde AND :hasta " +
           "ORDER BY r.fechaProximaAplicacion ASC")
    List<RegistroDesparasitacion> findProximasDesparasitaciones(@Param("companyId") Integer companyId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT r FROM RegistroDesparasitacion r " +
           "JOIN FETCH r.historiaClinica h JOIN FETCH h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId AND r.activo = true AND r.fechaProximaAplicacion IS NOT NULL " +
           "AND r.fechaProximaAplicacion BETWEEN :desde AND :hasta ORDER BY r.fechaProximaAplicacion ASC")
    List<RegistroDesparasitacion> findProximasDesparasitaciones(@Param("companyId") Integer companyId,
                                                                @Param("desde") LocalDate desde,
                                                                @Param("hasta") LocalDate hasta,
                                                                Pageable pageable);
}
