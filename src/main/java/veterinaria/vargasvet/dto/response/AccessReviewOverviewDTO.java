package veterinaria.vargasvet.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AccessReviewOverviewDTO(
        int intervalDays,
        boolean overdue,
        LocalDateTime nextDueAt,
        AccessReviewSummaryDTO lastReview,
        List<AccessReviewItemDTO> items) {
}
