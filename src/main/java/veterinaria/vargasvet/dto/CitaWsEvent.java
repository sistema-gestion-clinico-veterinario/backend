package veterinaria.vargasvet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import veterinaria.vargasvet.dto.response.CitaResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaWsEvent {
    private String tipo;
    private CitaResponse cita;
    private Integer companyId;
}
