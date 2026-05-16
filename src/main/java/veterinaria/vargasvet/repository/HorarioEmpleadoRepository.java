package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.HorarioEmpleado;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioEmpleadoRepository extends JpaRepository<HorarioEmpleado, Long> {
    List<HorarioEmpleado> findByEmpleadoId(Long empleadoId);
    
    List<HorarioEmpleado> findByEmpleadoIdAndFechaBetween(Long empleadoId, LocalDate start, LocalDate end);
    
    @Query("SELECT h FROM HorarioEmpleado h WHERE h.empleado.id = :empleadoId AND h.fecha = :fecha")
    List<HorarioEmpleado> findByEmpleadoIdAndFecha(@Param("empleadoId") Long empleadoId, @Param("fecha") LocalDate fecha);

    void deleteByEmpleadoId(Long empleadoId);
    
    void deleteByEmpleadoIdAndFechaBetween(Long empleadoId, LocalDate start, LocalDate end);

    @Query("SELECT COUNT(h) > 0 FROM HorarioEmpleado h WHERE h.empleado.id = :empleadoId " +
           "AND h.fecha = :fecha AND ((:inicio < h.horaFin AND :fin > h.horaInicio))")
    boolean existsOverlap(@Param("empleadoId") Long empleadoId, 
                          @Param("fecha") LocalDate fecha, 
                          @Param("inicio") LocalTime inicio, 
                          @Param("fin") LocalTime fin);
}
