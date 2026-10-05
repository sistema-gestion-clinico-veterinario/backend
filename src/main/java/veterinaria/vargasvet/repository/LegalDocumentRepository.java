package veterinaria.vargasvet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    Optional<LegalDocument> findByTipoAndActivoTrue(LegalDocumentType tipo);

    boolean existsByTipoAndVersion(LegalDocumentType tipo, String version);

    /** Bloquea la versión vigente mientras se publica la siguiente, para que no se publiquen dos a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ld FROM LegalDocument ld WHERE ld.tipo = :tipo AND ld.activo = true")
    Optional<LegalDocument> findActiveByTipoForUpdate(@Param("tipo") LegalDocumentType tipo);

    List<LegalDocument> findByActivoTrueOrderByIdAsc();
}
