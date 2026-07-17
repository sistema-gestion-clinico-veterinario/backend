package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistroDesparasitacionRequest {
    private Long controlPreventivoId;
    @NotBlank @Size(max = 100)
    private String producto;
    @NotNull
    private LocalDate fechaAplicacion;
    @NotNull @Min(1) @Max(120)
    private Integer periodicidadMeses;
    private LocalDate fechaProximaAplicacion;
}
