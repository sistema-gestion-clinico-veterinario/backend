package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReceiptResultRequest {
    @NotNull
    private Boolean generated;

    @Size(max = 500)
    private String error;
}
