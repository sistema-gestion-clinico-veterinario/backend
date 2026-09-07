package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.LegalDocument;
import veterinaria.vargasvet.domain.entity.UserConsent;

import java.util.List;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    List<UserConsent> findByUsuarioId(Integer usuarioId);

    boolean existsByUsuarioIdAndLegalDocumentId(Integer usuarioId, Long legalDocumentId);

    @Query("SELECT ld FROM LegalDocument ld WHERE ld.activo = true AND ld.id NOT IN " +
            "(SELECT uc.legalDocument.id FROM UserConsent uc WHERE uc.usuario.id = :usuarioId)")
    List<LegalDocument> findPendingActiveDocuments(Integer usuarioId);
}
