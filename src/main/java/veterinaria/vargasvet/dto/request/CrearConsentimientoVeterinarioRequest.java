package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearConsentimientoVeterinarioRequest(
        @NotNull Long mascotaId,
        Long consultaId,
        @NotNull Long apoderadoId,
        @NotBlank @Size(max = 200) String procedimiento,
        @NotBlank @Size(max = 5000) String diagnosticoIndicacion,
        @NotBlank @Size(max = 8000) String descripcionProcedimiento,
        @NotBlank @Size(max = 5000) String beneficiosEsperados,
        @NotBlank @Size(max = 8000) String riesgosPrevisibles,
        @NotBlank @Size(max = 5000) String alternativas,
        @NotBlank @Size(max = 5000) String consecuenciasNoRealizar,
        boolean autorizacionAnestesia,
        boolean autorizacionEmergencia) {
}
