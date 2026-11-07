package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.ClinicalTemplateSection;

public interface ClinicalTemplateSectionRepository extends JpaRepository<ClinicalTemplateSection, Long> {
}
