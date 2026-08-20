package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.math.BigDecimal;

@Data
public class TipoDesparasitanteRequest {
    @NotBlank @Size(max = 100)
    private String nombre;
    @NotNull
    private EspecieMascota especie;
    @Min(1) @Max(120)
    private Integer periodicidadMesesSugerida;
    @NotNull(message = "Debe indicar el precio")
    private BigDecimal precio;
}