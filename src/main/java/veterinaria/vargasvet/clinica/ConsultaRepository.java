package veterinaria.vargasvet.clinica;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.clinica.Consulta;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    Optional<Consulta> findByCitaId(Long citaId);

    @Query("SELECT c.historiaClinica.id, MAX(c.fechaConsulta) FROM Consulta c WHERE c.historiaClinica.id IN :ids GROUP BY c.historiaClinica.id")
    List<Object[]> findUltimasFechasConsulta(@Param("ids") List<Long> ids);
}
