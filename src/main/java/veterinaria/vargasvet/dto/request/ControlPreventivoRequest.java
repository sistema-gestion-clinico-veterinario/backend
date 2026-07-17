package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;

@Data
public class ControlPreventivoRequest {
    @NotNull
    private TipoControlPreventivo tipo;
    private Long tipoVacunaId;
    @Size(max = 100)
    private String nombreControl;
    @NotNull
    private LocalDate fechaRecomendada;
}
