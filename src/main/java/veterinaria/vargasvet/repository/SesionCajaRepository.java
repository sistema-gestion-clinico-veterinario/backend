package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;

import java.util.Optional;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {
    Optional<SesionCaja> findFirstByCompanyIdAndEstadoOrderByAbiertaAtDesc(Integer companyId, EstadoSesionCaja estado);
}
