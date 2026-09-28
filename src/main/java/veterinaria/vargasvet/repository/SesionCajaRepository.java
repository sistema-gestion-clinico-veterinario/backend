package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;

import java.util.Optional;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {
    Optional<SesionCaja> findFirstByCompanyIdAndEstadoOrderByAbiertaAtDesc(Integer companyId, EstadoSesionCaja estado);

    Page<SesionCaja> findByCompanyIdOrderByAbiertaAtDesc(Integer companyId, Pageable pageable);
}
