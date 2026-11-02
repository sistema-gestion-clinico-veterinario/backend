package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

public record SolicitarCambioCumplimientoRequest(
        @NotNull ControlCumplimientoClinica control,
        boolean habilitado,
        @NotNull ModoGestionCumplimiento modo,
        boolean bloqueante,
        @NotBlank @Size(min = 10, max = 2000) String motivo,
        @Size(max = 200) String evidenciaReferencia,
        LocalDateTime vigenteDesde) {
}
