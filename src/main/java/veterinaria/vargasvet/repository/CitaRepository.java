package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Cita;

import java.time.LocalDateTime;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
           "AND c.estado != 'CANCELADA' " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCita(
            @Param("veterinarioId") Long veterinarioId,
            @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
            @Param("fechaHoraFin") LocalDateTime fechaHoraFin
    );

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
           "AND c.estado != 'CANCELADA' " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaMascota(
            @Param("mascotaId") Long mascotaId,
            @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
            @Param("fechaHoraFin") LocalDateTime fechaHoraFin
    );
}
