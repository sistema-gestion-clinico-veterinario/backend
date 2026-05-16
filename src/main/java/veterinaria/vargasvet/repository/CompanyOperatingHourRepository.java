package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.CompanyOperatingHour;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyOperatingHourRepository extends JpaRepository<CompanyOperatingHour, Long> {
    List<CompanyOperatingHour> findByCompanyId(Integer companyId);
    Optional<CompanyOperatingHour> findByCompanyIdAndDiaSemana(Integer companyId, DiaSemana diaSemana);
}
