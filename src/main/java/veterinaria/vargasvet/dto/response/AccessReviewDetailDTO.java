package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.AccessReviewDecision;

import java.util.List;

public record AccessReviewDetailDTO(AccessReviewSummaryDTO review, List<Entry> entries) {

    public record Entry(
            String nombre,
            String email,
            String roleName,
            String reasons,
            AccessReviewDecision decision,
            String comment) {
    }
}
