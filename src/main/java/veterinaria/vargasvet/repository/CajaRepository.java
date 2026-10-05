package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.Caja;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    List<Caja> findByCompanyIdOrderByNombreAsc(Integer companyId);

    List<Caja> findByCompanyIdAndActivaTrueOrderByNombreAsc(Integer companyId);

    Optional<Caja> findByIdAndCompanyId(Long id, Integer companyId);

    Optional<Caja> findByDispositivoTokenHashAndCompanyId(String dispositivoTokenHash, Integer companyId);

    boolean existsByCompanyIdAndNombreIgnoreCase(Integer companyId, String nombre);
}
