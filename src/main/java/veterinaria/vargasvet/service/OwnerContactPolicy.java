package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;

/**
 * Los avisos con datos de una mascota o una cita solo se envían por correo a una dirección que la
 * propia persona ya confirmó. Un cliente registrado por recepción, pendiente de activar, tiene un
 * correo que alguien escribió a mano: si tuviera un error, el aviso llegaría a un desconocido. En
 * ese caso la clínica avisa por teléfono.
 */
@Component
@RequiredArgsConstructor
public class OwnerContactPolicy {

    private final UsuarioContactoService contactoService;

    public boolean puedeRecibirCorreo(Usuario usuario) {
        return usuario != null
                && usuario.isEmailVerified()
                && usuario.getEmail() != null
                && !usuario.getEmail().isBlank();
    }

    /** Teléfono de la persona en la empresa de ESTA relación de cliente. */
    public String telefono(Apoderado apoderado) {
        if (apoderado == null || apoderado.getUser() == null) return null;
        Integer companyId = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
        return contactoService.telefono(apoderado.getUser().getId(), companyId);
    }
}
