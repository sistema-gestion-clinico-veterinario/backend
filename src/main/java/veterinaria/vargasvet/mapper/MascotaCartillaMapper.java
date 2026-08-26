package veterinaria.vargasvet.mapper;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.dto.response.MascotaCartillaResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class MascotaCartillaMapper {

    public MascotaCartillaResponse toResponse(Mascota mascota, LocalDate fechaUltimaAplicacion, ControlPreventivo controlPendiente) {
        if (mascota == null) {
            return null;
        }

        MascotaCartillaResponse response = new MascotaCartillaResponse();
        response.setId(mascota.getId());
        response.setNombreCompleto(mascota.getNombreCompleto());
        response.setEspecie(mascota.getEspecie() != null ? mascota.getEspecie().name() : null);
        response.setActivo(mascota.getActivo());
        response.setFechaUltimaAplicacion(fechaUltimaAplicacion);

        if (mascota.getRaza() != null) {
            response.setRazaNombre(mascota.getRaza().getNombre());
        }

        if (mascota.getApoderado() != null && mascota.getApoderado().getUser() != null) {
            String apoderadoNombre = mascota.getApoderado().getUser().getNombre() + " " +
                                     mascota.getApoderado().getUser().getApellido();
            response.setApoderadoNombreCompleto(apoderadoNombre);
            response.setApoderadoTelefono(mascota.getApoderado().getUser().getTelefono());
            response.setApoderadoId(mascota.getApoderado().getId());
        }

        if (controlPendiente != null) {
            response.setControlPendienteId(controlPendiente.getId());
            response.setControlPendienteNombre(controlPendiente.getNombreControl());
            response.setControlPendienteTipo(controlPendiente.getTipo() != null ? controlPendiente.getTipo().name() : null);
            response.setControlPendienteFecha(controlPendiente.getFechaRecomendada());
            response.setControlPendienteEstado(controlPendiente.getEstado() != null ? controlPendiente.getEstado().name() : null);

            LocalDate hoy = LocalDate.now();
            long diasRestantes = ChronoUnit.DAYS.between(hoy, controlPendiente.getFechaRecomendada());
            response.setControlPendienteDiasRestantes((int) diasRestantes);

            if (diasRestantes < 0) {
                response.setControlPendienteResumen("Venció hace " + Math.abs(diasRestantes) + " día(s)");
            } else if (diasRestantes == 0) {
                response.setControlPendienteResumen("Vence hoy");
            } else {
                response.setControlPendienteResumen("En " + diasRestantes + " día(s)");
            }
        }

        return response;
    }
}
