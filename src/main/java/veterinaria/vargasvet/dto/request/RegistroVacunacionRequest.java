package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegistroVacunacionRequest {
    private Long controlPreventivoId;
    @NotNull
    private Long tipoVacunaId;
    @NotNull
    private LocalDate fechaAplicacion;
    @NotNull @Min(1) @Max(120)
    private Integer periodicidadMeses;
    private LocalDate fechaProximaDosis;
}
