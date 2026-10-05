package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.CanalConsentimiento;

import java.time.LocalDateTime;
import java.util.List;

public record ConsentimientoEstadoResponse(boolean avisoPublicado, Integer avisoVersion, boolean informada,
                                           Integer informadaVersion, LocalDateTime informadaFecha,
                                           CanalConsentimiento informadaCanal, List<Finalidad> finalidades) {

    public record Finalidad(String codigo, String descripcion, String estado, LocalDateTime fecha,
                            CanalConsentimiento canal) {
    }
}
