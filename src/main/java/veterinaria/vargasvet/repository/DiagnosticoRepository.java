package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Diagnostico;

import java.util.List;

@Repository
public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Long> {

    @Query("SELECT d.tipo, d.estado, COUNT(d) FROM Diagnostico d " +
           "JOIN d.consulta c JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId " +
           "GROUP BY d.tipo, d.estado")
    List<Object[]> countByTipoYEstado(@Param("companyId") Integer companyId);
}
