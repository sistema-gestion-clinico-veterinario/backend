package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.CompanyCompliancePolicy;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanyCompliancePolicyRepository extends JpaRepository<CompanyCompliancePolicy, Long> {
    @Query("SELECT p FROM CompanyCompliancePolicy p WHERE p.company.id = :companyId AND p.control = :control " +
            "AND p.vigenteDesde <= :ahora ORDER BY p.version DESC")
    List<CompanyCompliancePolicy> versionesVigentes(@Param("companyId") Integer companyId,
                                                    @Param("control") ControlCumplimientoClinica control,
                                                    @Param("ahora") LocalDateTime ahora);

    default Optional<CompanyCompliancePolicy> vigente(Integer companyId, ControlCumplimientoClinica control,
                                                       LocalDateTime ahora) {
        return versionesVigentes(companyId, control, ahora).stream().findFirst();
    }

    List<CompanyCompliancePolicy> findByCompanyIdOrderByControlAscVersionDesc(Integer companyId);

    @Query("SELECT COALESCE(MAX(p.version), 0) FROM CompanyCompliancePolicy p " +
            "WHERE p.company.id = :companyId AND p.control = :control")
    int ultimaVersion(@Param("companyId") Integer companyId,
                      @Param("control") ControlCumplimientoClinica control);
}
