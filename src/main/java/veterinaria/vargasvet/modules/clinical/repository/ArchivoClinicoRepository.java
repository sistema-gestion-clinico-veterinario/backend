package veterinaria.vargasvet.modules.clinical.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.clinical.domain.entity.ArchivoClinico;

import java.util.List;

@Repository
public interface ArchivoClinicoRepository extends JpaRepository<ArchivoClinico, Long> {
    List<ArchivoClinico> findByConsultaId(Long consultaId);
}
