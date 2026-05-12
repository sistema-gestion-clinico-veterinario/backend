package veterinaria.vargasvet.clinica;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.clinica.ArchivoClinico;

import java.util.List;

@Repository
public interface ArchivoClinicoRepository extends JpaRepository<ArchivoClinico, Long> {
    List<ArchivoClinico> findByConsultaId(Long consultaId);
}
