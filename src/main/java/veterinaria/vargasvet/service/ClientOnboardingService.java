package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.dto.request.AltaClienteRequest;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.request.PrimeraMascotaRequest;
import veterinaria.vargasvet.dto.response.AltaClienteResponse;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

@Service
@RequiredArgsConstructor
public class ClientOnboardingService {
    private final ApoderadoService apoderadoService;
    private final MascotaService mascotaService;

    /**
     * Confirma el alta únicamente cuando responsable y primera mascota son válidos.
     * El aviso de privacidad se entrega AFTER_COMMIT, por lo que un error en la
     * mascota revierte también al usuario y no genera correos inconsistentes.
     */
    @Transactional
    public AltaClienteResponse registrar(AltaClienteRequest request) {
        UserProfileDTO cliente = apoderadoService.registerApoderado(request.getCliente());
        MascotaRequest mascotaRequest = toMascotaRequest(request.getMascota(), cliente.getApoderadoId());
        MascotaResponse mascota = mascotaService.registerMascota(mascotaRequest);
        return new AltaClienteResponse(cliente, mascota);
    }

    private MascotaRequest toMascotaRequest(PrimeraMascotaRequest source, Integer apoderadoId) {
        MascotaRequest target = new MascotaRequest();
        target.setNombreCompleto(source.getNombreCompleto());
        target.setEspecie(source.getEspecie());
        target.setOtraEspecie(source.getOtraEspecie());
        target.setRazaId(source.getRazaId());
        target.setSexo(source.getSexo());
        target.setFechaNacimiento(source.getFechaNacimiento());
        target.setPeso(source.getPeso());
        target.setColor(source.getColor());
        target.setApoderadoId(apoderadoId.longValue());
        return target;
    }
}
