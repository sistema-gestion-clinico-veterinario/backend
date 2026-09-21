package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Tratamiento;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    @Query("SELECT t.estado, COUNT(t) FROM Tratamiento t " +
           "JOIN t.consulta c JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE a.company.id = :companyId " +
           "GROUP BY t.estado")
    List<Object[]> countPorEstado(@Param("companyId") Integer companyId);

    List<Tratamiento> findByConsultaIdOrderById(Long consultaId);

    @Query("SELECT t FROM Tratamiento t " +
           "JOIN FETCH t.consulta c " +
           "WHERE c.historiaClinica.mascota.id = :mascotaId " +
           "ORDER BY c.fechaConsulta DESC")
    List<Tratamiento> findByMascotaIdOrderByFechaConsultaDesc(@Param("mascotaId") Long mascotaId);

    @Query("SELECT t FROM Tratamiento t " +
           "JOIN FETCH t.consulta c JOIN FETCH c.historiaClinica hc JOIN FETCH hc.mascota m JOIN FETCH m.apoderado a " +
           "WHERE a.company.id = :companyId AND t.estado IN ('ACTIVO', 'EN_CURSO') " +
           "AND t.fechaFin IS NOT NULL AND t.fechaFin BETWEEN :desde AND :hasta " +
           "ORDER BY t.fechaFin ASC")
    List<Tratamiento> findProximosAFinalizar(@Param("companyId") Integer companyId,
                                             @Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta);
}
