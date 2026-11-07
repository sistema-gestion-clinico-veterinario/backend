package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.ClinicalTemplate;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;

import java.util.List;
import java.util.Optional;

public interface ClinicalTemplateRepository extends JpaRepository<ClinicalTemplate, Long> {
    Optional<ClinicalTemplate> findFirstByCompanyIdAndTemplateKeyAndStatus(
            Integer companyId, String templateKey, ClinicalTemplateStatus status);

    Optional<ClinicalTemplate> findByIdAndCompanyId(Long id, Integer companyId);

    List<ClinicalTemplate> findByCompanyIdOrderByTemplateKeyAscVersionDesc(Integer companyId);

    int countByCompanyIdAndTemplateKey(Integer companyId, String templateKey);
}
