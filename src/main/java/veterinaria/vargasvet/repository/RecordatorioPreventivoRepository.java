package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecordatorioPreventivoRepository extends JpaRepository<RecordatorioPreventivo, Long> {
    boolean existsByControlPreventivoIdAndTipoAviso(Long controlId, TipoAvisoRecordatorio tipoAviso);
    Optional<RecordatorioPreventivo> findTopByApoderadoIdOrderByFechaEnvioDesc(Long apoderadoId);
    long countByApoderadoIdAndFechaEnvioAfter(Long apoderadoId, LocalDateTime desde);
}
