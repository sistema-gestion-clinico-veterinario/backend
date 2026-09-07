package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    Optional<LegalDocument> findByTipoAndActivoTrue(LegalDocumentType tipo);

    List<LegalDocument> findByActivoTrue();
}
