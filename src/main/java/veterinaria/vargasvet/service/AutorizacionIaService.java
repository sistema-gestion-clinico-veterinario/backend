package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.ConsentimientoDatos;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EstadoConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.dto.response.AutorizacionIaResponse;
import veterinaria.vargasvet.exception.AutorizacionIaRequeridaException;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ConsentimientoDatosRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.Optional;

/**
 * La IA solo se usa con los datos clínicos de una mascota si su titular (el cliente principal) la autorizó de forma
 * expresa y separada de los términos. A diferencia de los recordatorios, aquí no hay autorización por omisión: sin
 * constancia vigente, o con la última constancia retirada, no se usa.
 */
@Service
@RequiredArgsConstructor
public class AutorizacionIaService {

    private final MascotaRepository mascotaRepository;
    private final ConsentimientoDatosRepository consentimientoRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AutorizacionIaResponse estado(Long mascotaId) {
        Integer companyId = companyIdActual();
        Mascota mascota = mascotaRepository.findByIdAndCompanyId(mascotaId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        return estadoDe(mascota, companyId);
    }

    @Transactional(readOnly = true)
    public AutorizacionIaResponse exigir(Long mascotaId) {
        AutorizacionIaResponse estado = estado(mascotaId);
        if (!estado.autorizada()) {
            throw new AutorizacionIaRequeridaException(estado.apoderadoId());
        }
        return estado;
    }

    public void registrarUso(Long mascotaId, String funcion, AutorizacionIaResponse autorizacion) {
        auditLogService.log("USAR_IA_CLINICA", "Inteligencia artificial",
                "Se enviaron datos clínicos de la mascota con ID " + mascotaId + " al servicio de IA (" + funcion
                        + "), con autorización vigente de " + autorizacion.titular() + " registrada el "
                        + autorizacion.fecha() + " por canal " + autorizacion.canal() + ".");
    }

    private AutorizacionIaResponse estadoDe(Mascota mascota, Integer companyId) {
        var apoderado = mascota.getApoderado();
        var titular = apoderado.getUser();
        Optional<ConsentimientoDatos> ultima = consentimientoRepository
                .findFirstByUsuarioIdAndCompanyIdAndFinalidadOrderByIdDesc(
                        titular.getId(), companyId, FinalidadDatos.USO_IA_CLINICA);
        boolean autorizada = ultima.isPresent() && ultima.get().getEstado() == EstadoConsentimiento.OTORGADO;
        String nombre = (titular.getNombre() + " " + titular.getApellido()).strip();
        return new AutorizacionIaResponse(autorizada, apoderado.getId(), nombre,
                autorizada ? ultima.get().getFecha() : null, autorizada ? ultima.get().getCanal() : null);
    }

    private Integer companyIdActual() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("Esta acción corresponde a una clínica; ingresa con la cuenta de la clínica");
        }
        return companyId;
    }
}
