package veterinaria.vargasvet.dto.response;

import java.time.LocalDateTime;

public record AccessReviewSummaryDTO(
        Long id,
        LocalDateTime performedAt,
        String performedByEmail,
        int reviewedCount,
        int keptCount,
        int revokedCount,
        String notes) {
}
