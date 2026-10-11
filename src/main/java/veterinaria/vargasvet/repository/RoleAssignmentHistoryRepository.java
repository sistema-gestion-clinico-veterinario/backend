package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.RoleAssignmentHistory;

public interface RoleAssignmentHistoryRepository extends JpaRepository<RoleAssignmentHistory, Long> {
}
