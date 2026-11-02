package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

public record DetalleCumplimientoClinicaResponse(
        ControlCumplimientoClinica control,
        int version,
        boolean habilitado,
        ModoGestionCumplimiento modo,
        boolean bloqueante,
        boolean gestionadoPorSoftvet,
        String justificacion,
        String referenciaContractual,
        String configuradaPor,
        boolean configuracionExplicita) {

    public static DetalleCumplimientoClinicaResponse desde(ConfiguracionCumplimientoResponse configuracion) {
        return new DetalleCumplimientoClinicaResponse(
                configuracion.control(), configuracion.version(), configuracion.habilitado(), configuracion.modo(),
                configuracion.bloqueante(), configuracion.gestionadoPorSoftvet(), configuracion.justificacion(),
                configuracion.referenciaContractual(), configuracion.configuradaPor(), configuracion.configuracionExplicita());
    }
}
