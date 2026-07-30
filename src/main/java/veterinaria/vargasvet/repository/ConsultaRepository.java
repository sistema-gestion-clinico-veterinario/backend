package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Consulta;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    Optional<Consulta> findByCitaId(Long citaId);

    @Query("SELECT c.historiaClinica.id, MAX(c.fechaConsulta) FROM Consulta c WHERE c.historiaClinica.id IN :ids GROUP BY c.historiaClinica.id")
    List<Object[]> findUltimasFechasConsulta(@Param("ids") List<Long> ids);

    @Query("SELECT c.tipoConsulta, COUNT(c) FROM Consulta c " +
           "JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId " +
           "GROUP BY c.tipoConsulta")
    List<Object[]> countByTipoConsulta(@Param("companyId") Integer companyId);

    @Query("SELECT c.estado, COUNT(c) FROM Consulta c " +
           "JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE u.company.id = :companyId " +
           "GROUP BY c.estado")
    List<Object[]> countPorEstado(@Param("companyId") Integer companyId);
}
