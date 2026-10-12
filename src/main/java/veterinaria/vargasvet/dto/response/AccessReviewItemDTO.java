package veterinaria.vargasvet.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AccessReviewItemDTO(
        Integer assignmentId,
        Integer usuarioId,
        String nombre,
        String email,
        Integer roleId,
        String roleName,
        List<String> reasons,
        Instant lastActivity,
        LocalDateTime assignedAt) {
}
