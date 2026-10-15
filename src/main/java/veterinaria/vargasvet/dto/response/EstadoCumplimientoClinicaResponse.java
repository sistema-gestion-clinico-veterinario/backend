package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

public record EstadoCumplimientoClinicaResponse(
        ControlCumplimientoClinica control,
        boolean habilitado,
        ModoGestionCumplimiento modo,
        boolean bloqueante,
        boolean gestionadoPorSoftvet) {

    public static EstadoCumplimientoClinicaResponse desde(ConfiguracionCumplimientoResponse configuracion) {
        return new EstadoCumplimientoClinicaResponse(configuracion.control(), configuracion.habilitado(),
                configuracion.modo(), configuracion.bloqueante(), configuracion.gestionadoPorSoftvet());
    }
}
