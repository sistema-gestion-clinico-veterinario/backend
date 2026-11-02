package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

public record SolicitudCambioCumplimientoResponse(
        Long id, Integer companyId, String empresa, ControlCumplimientoClinica control,
        boolean habilitadoSolicitado, ModoGestionCumplimiento modoSolicitado,
        boolean bloqueanteSolicitado, String motivo, String evidenciaReferencia,
        LocalDateTime vigenteDesdeSolicitada, EstadoSolicitudCambioCumplimiento estado,
        String solicitadaPor, LocalDateTime solicitadaAt, String resueltaPor,
        LocalDateTime resueltaAt, String comentarioResolucion,
        String revisionPor, LocalDateTime revisionAt, LocalDateTime vistaClinicaAt) {
}
