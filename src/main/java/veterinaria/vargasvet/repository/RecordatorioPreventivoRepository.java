package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;

public interface RecordatorioPreventivoRepository extends JpaRepository<RecordatorioPreventivo, Long> {
    boolean existsByControlPreventivoIdAndTipoAvisoAndFechaProgramada(
            Long controlId, TipoAvisoRecordatorio tipoAviso, java.time.LocalDate fechaProgramada);
    boolean existsByControlPreventivoIdAndTipoAviso(
            Long controlId, TipoAvisoRecordatorio tipoAviso);
    long countByApoderadoIdAndFechaEnvioAfter(
            Long apoderadoId, java.time.LocalDateTime fechaEnvio);
}
