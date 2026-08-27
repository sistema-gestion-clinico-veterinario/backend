package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RecordatorioPreventivoRepository extends JpaRepository<RecordatorioPreventivo, Long> {
    interface ReminderKey {
        Long getControlId();
        TipoAvisoRecordatorio getTipoAviso();
        LocalDate getFechaProgramada();
    }

    @Query("""
            select r.controlPreventivo.id as controlId, r.tipoAviso as tipoAviso,
                   r.fechaProgramada as fechaProgramada
            from RecordatorioPreventivo r
            where r.controlPreventivo.id in :controlIds
            """)
    List<ReminderKey> findExistingKeys(@Param("controlIds") Collection<Long> controlIds);

    @Query("""
            select distinct r.apoderado.id
            from RecordatorioPreventivo r
            where r.apoderado.id in :apoderadoIds and r.fechaEnvio > :desde
            """)
    List<Long> findApoderadoIdsWithRecentReminder(
            @Param("apoderadoIds") Collection<Long> apoderadoIds,
            @Param("desde") LocalDateTime desde);
}
