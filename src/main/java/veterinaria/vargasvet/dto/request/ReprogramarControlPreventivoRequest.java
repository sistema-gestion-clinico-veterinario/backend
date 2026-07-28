package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReprogramarControlPreventivoRequest {
    @NotNull
    @FutureOrPresent
    private LocalDate fechaRecomendada;
}
