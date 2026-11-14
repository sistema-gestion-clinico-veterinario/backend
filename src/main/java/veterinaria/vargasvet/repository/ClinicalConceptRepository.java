package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ClinicalConcept;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClinicalConceptRepository extends JpaRepository<ClinicalConcept, Long> {
    @Query("SELECT c FROM ClinicalConcept c WHERE c.active = true AND (c.company IS NULL OR c.company.id = :companyId) ORDER BY c.baseName")
    List<ClinicalConcept> findAvailableForCompany(@Param("companyId") Integer companyId);

    @Query("SELECT c FROM ClinicalConcept c WHERE c.id IN :ids AND (c.company IS NULL OR c.company.id = :companyId)")
    List<ClinicalConcept> findAccessibleByIds(@Param("ids") Collection<Long> ids, @Param("companyId") Integer companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(Integer companyId, String code);

    Optional<ClinicalConcept> findByCompanyIsNullAndCode(String code);

    List<ClinicalConcept> findByCompanyIsNullAndCodeIn(java.util.Collection<String> codes);
}
