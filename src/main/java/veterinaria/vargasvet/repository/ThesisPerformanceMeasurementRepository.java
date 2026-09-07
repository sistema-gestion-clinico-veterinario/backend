package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.ThesisPerformanceMeasurement;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThesisPerformanceMeasurementRepository extends JpaRepository<ThesisPerformanceMeasurement, Long> {
    List<ThesisPerformanceMeasurement> findAllByMeasurementSessionIdAndUserIdOrderByRequestedAtAscIdAsc(
            UUID measurementSessionId,
            Integer userId);
}
