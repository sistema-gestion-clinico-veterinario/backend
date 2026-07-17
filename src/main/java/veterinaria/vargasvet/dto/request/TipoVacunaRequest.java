package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

@Data
public class TipoVacunaRequest {
    @NotBlank @Size(max = 100)
    private String nombre;
    @NotNull
    private EspecieMascota especie;
    @Min(1) @Max(120)
    private Integer periodicidadMesesSugerida;
}
