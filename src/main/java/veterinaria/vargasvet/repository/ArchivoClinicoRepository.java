package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.ArchivoClinico;

import java.util.List;

@Repository
public interface ArchivoClinicoRepository extends JpaRepository<ArchivoClinico, Long> {
    List<ArchivoClinico> findByConsultaId(Long consultaId);
}
