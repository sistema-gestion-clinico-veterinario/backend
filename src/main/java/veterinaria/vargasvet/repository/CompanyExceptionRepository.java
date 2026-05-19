package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.CompanyException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyExceptionRepository extends JpaRepository<CompanyException, Long> {
    List<CompanyException> findByCompanyId(Integer companyId);
    Optional<CompanyException> findByCompanyIdAndDate(Integer companyId, LocalDate date);

    @Query("SELECT ce FROM CompanyException ce WHERE ce.company.id = :companyId AND ce.date = CAST(:date AS date)")
    Optional<CompanyException> findByCompanyIdAndDateString(@Param("companyId") Integer companyId, @Param("date") String date);
}
