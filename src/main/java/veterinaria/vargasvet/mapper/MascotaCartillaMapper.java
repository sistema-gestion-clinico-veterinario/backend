package veterinaria.vargasvet.mapper;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.dto.response.MascotaCartillaResponse;

import java.time.LocalDate;

@Component
public class MascotaCartillaMapper {

    public MascotaCartillaResponse toResponse(Mascota mascota, LocalDate fechaUltimaAplicacion) {
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
        }

        return response;
    }
}
