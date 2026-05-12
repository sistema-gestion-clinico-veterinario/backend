package veterinaria.vargasvet.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.admin.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
}
