package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import veterinaria.vargasvet.domain.enums.AccessReviewDecision;

import java.util.List;

public record AccessReviewRequest(
        @NotEmpty(message = "Debe decidir sobre al menos un acceso") @Valid List<Decision> decisions,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres") String notes) {

    public record Decision(
            @NotNull(message = "Falta el acceso a decidir") Integer assignmentId,
            @NotNull(message = "Falta la decisión") AccessReviewDecision decision,
            @Size(max = 300, message = "El comentario no puede superar 300 caracteres") String comment) {
    }
}
