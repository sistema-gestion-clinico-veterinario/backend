package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.ApoderadoEstadoResponse;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface ApoderadoService {
    UserProfileDTO registerApoderado(ApoderadoRequest dto);
    void enviarInvitacionAccesoSiTieneMascota(Long apoderadoId);

    /** Invita a una persona activa de la clínica a entrar al portal, aunque no tenga mascotas propias. */
    void invitarAcceso(Long apoderadoId);
    UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto);
    ApoderadoEstadoResponse cambiarEstado(Long id, Boolean nuevoEstado, TipoInactividad tipo, String motivo);

    /** Genera un enlace de activación nuevo y lo envía al correo del cliente con cuenta pendiente. */
    void reenviarInvitacion(Long id);
    ApoderadoEstadoResponse eliminar(Long id);
    Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size);
    ApoderadoRequest findById(Long id);
}
