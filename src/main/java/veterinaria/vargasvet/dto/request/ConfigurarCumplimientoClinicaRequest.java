package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

public record ConfigurarCumplimientoClinicaRequest(
        boolean habilitado,
        @NotNull ModoGestionCumplimiento modo,
        boolean bloqueante,
        @NotBlank @Size(min = 10, max = 4000) String justificacion,
        @Size(max = 200) String referenciaContractual,
        boolean confirmoRevisionLegal,
        LocalDateTime vigenteDesde) {
}
