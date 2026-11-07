package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.SystemLog;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
}
