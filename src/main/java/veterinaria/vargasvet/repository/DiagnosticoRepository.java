package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Diagnostico;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Long> {

    @Query("SELECT d.tipo, d.estado, COUNT(d) FROM Diagnostico d " +
           "JOIN d.consulta c JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE a.company.id = :companyId " +
           "GROUP BY d.tipo, d.estado")
    List<Object[]> countByTipoYEstado(@Param("companyId") Integer companyId);

    List<Diagnostico> findByConsultaIdOrderById(Long consultaId);

    @Query("SELECT d FROM Diagnostico d " +
           "JOIN FETCH d.consulta c " +
           "WHERE c.historiaClinica.mascota.id = :mascotaId " +
           "ORDER BY c.fechaConsulta DESC")
    List<Diagnostico> findByMascotaIdOrderByFechaConsultaDesc(@Param("mascotaId") Long mascotaId);

    @Query("SELECT d FROM Diagnostico d " +
           "JOIN FETCH d.consulta c JOIN FETCH c.historiaClinica hc JOIN FETCH hc.mascota m JOIN FETCH m.apoderado a " +
           "WHERE a.company.id = :companyId AND d.estado IN ('EN_SEGUIMIENTO', 'CRONICO') " +
           "AND d.fechaProximoControl IS NOT NULL AND d.fechaProximoControl BETWEEN :desde AND :hasta " +
           "ORDER BY d.fechaProximoControl ASC")
    List<Diagnostico> findProximosControles(@Param("companyId") Integer companyId,
                                            @Param("desde") LocalDate desde,
                                            @Param("hasta") LocalDate hasta);
}
