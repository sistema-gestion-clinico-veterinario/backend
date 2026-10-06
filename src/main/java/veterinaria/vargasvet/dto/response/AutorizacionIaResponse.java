package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.CanalConsentimiento;

import java.time.LocalDateTime;

public record AutorizacionIaResponse(boolean autorizada, Long apoderadoId, String titular,
                                     LocalDateTime fecha, CanalConsentimiento canal) {}
