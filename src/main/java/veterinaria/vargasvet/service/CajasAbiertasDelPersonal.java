package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Caja;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.repository.CajaRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CajasAbiertasDelPersonal {

    private static final String CAJA_SIN_NOMBRE = "Caja";

    private final SesionCajaRepository sesionCajaRepository;
    private final CajaRepository cajaRepository;

    /** Nombres de las cajas que esta persona dejó abiertas en la empresa, para avisar que un administrador debe cerrarlas. */
    public List<String> nombres(Usuario usuario, Integer companyId) {
        return sesionCajaRepository.findAllByCompanyIdAndEstado(companyId, EstadoSesionCaja.ABIERTA).stream()
                .filter(sesion -> abiertaPor(sesion, usuario))
                .map(this::nombreDeLaCaja)
                .distinct()
                .toList();
    }

    private boolean abiertaPor(SesionCaja sesion, Usuario usuario) {
        if (sesion.getAbiertaPorUsuarioId() != null) {
            return sesion.getAbiertaPorUsuarioId().equals(usuario.getId());
        }
        return usuario.getEmail() != null && usuario.getEmail().equalsIgnoreCase(sesion.getAbiertaPor());
    }

    private String nombreDeLaCaja(SesionCaja sesion) {
        if (sesion.getCajaId() == null) {
            return CAJA_SIN_NOMBRE;
        }
        return cajaRepository.findById(sesion.getCajaId()).map(Caja::getNombre).orElse(CAJA_SIN_NOMBRE);
    }
}
