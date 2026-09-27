package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AlertaVencimientoResponse {
    private int vencidosCount;
    private int porVencerCount;
    private List<LoteResponse> vencidos;
    private List<LoteResponse> porVencer;
}
