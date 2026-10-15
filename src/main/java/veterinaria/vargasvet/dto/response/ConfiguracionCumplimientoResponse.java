package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

public record ConfiguracionCumplimientoResponse(
        ControlCumplimientoClinica control, int version, boolean habilitado,
        ModoGestionCumplimiento modo, boolean bloqueante, String justificacion,
        String referenciaContractual, LocalDateTime vigenteDesde, LocalDateTime creadaAt,
        String configuradaPor, boolean configuracionExplicita) {

    public boolean gestionadoPorSoftvet() {
        return habilitado && modo == ModoGestionCumplimiento.SOFTVET;
    }
}
