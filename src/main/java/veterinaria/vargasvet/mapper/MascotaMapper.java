package veterinaria.vargasvet.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;

@Component
@RequiredArgsConstructor
public class MascotaMapper {

    private final HistoriaClinicaRepository historiaClinicaRepository;

    public MascotaResponse toResponse(Mascota mascota) {
        if (mascota == null) {
            return null;
        }

        MascotaResponse response = new MascotaResponse();
        response.setId(mascota.getId());
        response.setUuid(mascota.getUuid());
        response.setNombreCompleto(mascota.getNombreCompleto());
        response.setEspecie(mascota.getEspecie());
        response.setOtraEspecie(mascota.getOtraEspecie());

        response.setRaza(mascota.getRaza());
        response.setSexo(mascota.getSexo());
        response.setFechaNacimiento(mascota.getFechaNacimiento());
        response.setPeso(mascota.getPeso());
        response.setColor(mascota.getColor());
        response.setSenasParticulares(mascota.getSenasParticulares());
        response.setEsterilizado(mascota.getEsterilizado());
        response.setActivo(mascota.getActivo());
        response.setFotoUrl(mascota.getFotoUrl());
        response.setNumeroMicrochip(mascota.getNumeroMicrochip());
        response.setObservaciones(mascota.getObservaciones());

        if (mascota.getApoderado() != null && mascota.getApoderado().getUser() != null) {
            response.setApoderadoId(mascota.getApoderado().getId());
            String apoderadoNombre = mascota.getApoderado().getUser().getNombre() + " " +
                                     mascota.getApoderado().getUser().getApellido();
            response.setApoderadoNombreCompleto(apoderadoNombre);
        }

        response.setTieneHistoriaClinica(historiaClinicaRepository.existsByMascotaId(mascota.getId()));

        return response;
    }
}
