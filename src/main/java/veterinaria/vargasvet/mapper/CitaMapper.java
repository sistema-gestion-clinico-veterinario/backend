package veterinaria.vargasvet.mapper;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.dto.response.CitaResponse;

@Component
public class CitaMapper {

    public CitaResponse toResponse(Cita cita) {
        if (cita == null) {
            return null;
        }

        CitaResponse response = new CitaResponse();
        response.setId(cita.getId());
        response.setVersion(cita.getVersion());
        
        if (cita.getMascota() != null) {
            response.setMascotaId(cita.getMascota().getId());
            response.setMascotaNombre(cita.getMascota().getNombreCompleto());
            if (cita.getMascota().getApoderado() != null) {
                response.setApoderadoId(cita.getMascota().getApoderado().getId());
                if (cita.getMascota().getApoderado().getUser() != null) {
                    response.setApoderadoNombre(cita.getMascota().getApoderado().getUser().getNombre() + " " + cita.getMascota().getApoderado().getUser().getApellido());
                }
            }
        }

        if (cita.getEmpleado() != null) {
            response.setVeterinarioId(cita.getEmpleado().getId());
            if (cita.getEmpleado().getUser() != null) {
                response.setVeterinarioNombre(cita.getEmpleado().getUser().getNombre() + " " + cita.getEmpleado().getUser().getApellido());
            }
        }

        if (cita.getServicio() != null) {
            response.setServicioId(cita.getServicio().getId());
            response.setServicioNombre(cita.getServicio().getNombre());
        }

        response.setMotivoCita(cita.getMotivoCita());
        response.setFechaHoraInicio(cita.getFechaHoraInicio());
        response.setFechaHoraFin(cita.getFechaHoraFin());
        response.setDuracionMinutos(cita.getDuracionMinutos());
        response.setEstado(cita.getEstado());
        response.setNotas(cita.getNotas());
        
        if (cita.getConsulta() != null) {
            response.setConsultaId(cita.getConsulta().getId());
        }

        return response;
    }
}
