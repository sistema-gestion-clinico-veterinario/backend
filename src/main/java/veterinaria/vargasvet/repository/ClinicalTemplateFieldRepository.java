package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ClinicalTemplateField;

import java.util.Optional;

public interface ClinicalTemplateFieldRepository extends JpaRepository<ClinicalTemplateField, Long> {
    @Query("SELECT f FROM ClinicalTemplateField f JOIN FETCH f.concept JOIN FETCH f.section s JOIN FETCH s.template t " +
            "WHERE f.id = :id AND t.company.id = :companyId")
    Optional<ClinicalTemplateField> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Integer companyId);
}
