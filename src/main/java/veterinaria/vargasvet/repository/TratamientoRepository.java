package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Tratamiento;

import java.util.List;

@Repository
public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    @Query("SELECT t.estado, COUNT(t) FROM Tratamiento t " +
           "JOIN t.consulta c JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId " +
           "GROUP BY t.estado")
    List<Object[]> countPorEstado(@Param("companyId") Integer companyId);
}
