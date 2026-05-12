package veterinaria.vargasvet.modules.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.company.domain.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
}
