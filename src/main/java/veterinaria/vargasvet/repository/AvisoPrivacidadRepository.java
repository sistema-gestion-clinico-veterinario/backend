package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisoPrivacidadRepository extends JpaRepository<AvisoPrivacidad, Long> {

    Optional<AvisoPrivacidad> findByCompanyIdAndActivoTrue(Integer companyId);

    List<AvisoPrivacidad> findByCompanyIdOrderByVersionDesc(Integer companyId);

    @Query("SELECT COALESCE(MAX(a.version), 0) FROM AvisoPrivacidad a WHERE a.company.id = :companyId")
    int ultimaVersion(@Param("companyId") Integer companyId);
}
