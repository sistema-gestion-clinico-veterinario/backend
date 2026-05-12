package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.Apoderado;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {
    Optional<Apoderado> findByUserId(Integer userId);
    List<Apoderado> findByUserCompanyId(Integer companyId);
    boolean existsByNumeroDocumento(String numeroDocumento);
}
