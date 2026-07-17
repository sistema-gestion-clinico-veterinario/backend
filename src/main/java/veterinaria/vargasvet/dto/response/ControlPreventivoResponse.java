package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;

@Data
@Builder
public class ControlPreventivoResponse {
    private Long id;
    private Long mascotaId;
    private TipoControlPreventivo tipo;
    private Long tipoVacunaId;
    private String nombreControl;
    private LocalDate fechaRecomendada;
    private EstadoControlPreventivo estado;
    private Long citaSuspendeId;
}
