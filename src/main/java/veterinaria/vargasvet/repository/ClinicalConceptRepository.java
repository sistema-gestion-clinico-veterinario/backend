package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ClinicalConcept;

import java.util.List;
import java.util.Optional;

public interface ClinicalConceptRepository extends JpaRepository<ClinicalConcept, Long> {
    @Query("SELECT c FROM ClinicalConcept c WHERE c.active = true AND (c.company IS NULL OR c.company.id = :companyId) ORDER BY c.baseName")
    List<ClinicalConcept> findAvailableForCompany(@Param("companyId") Integer companyId);

    @Query("SELECT c FROM ClinicalConcept c WHERE c.id = :id AND (c.company IS NULL OR c.company.id = :companyId)")
    Optional<ClinicalConcept> findAccessibleById(@Param("id") Long id, @Param("companyId") Integer companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(Integer companyId, String code);

    Optional<ClinicalConcept> findByCompanyIsNullAndCode(String code);
}
