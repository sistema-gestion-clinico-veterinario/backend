package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PermisosDelRol {

    private static final String ATRIBUTO_PERMISOS_DEL_ROL = PermisosDelRol.class.getName() + ".rol.";

    private final RolVistaPermisoRepository rolVistaPermisoRepository;

    public Optional<RolVistaPermiso> de(Integer rolId, String codigoDeVista) {
        if (!MemoDeLaSolicitud.hayUnaSolicitudActiva()) {
            return rolVistaPermisoRepository.findByRolIdAndVistaCodigo(rolId, codigoDeVista);
        }
        return MemoDeLaSolicitud.obtener(ATRIBUTO_PERMISOS_DEL_ROL + rolId, PermisosLeidos.class,
                () -> new PermisosLeidos(rolId, rolVistaPermisoRepository)).de(codigoDeVista);
    }

    private static final class PermisosLeidos {

        private final Integer rolId;
        private final RolVistaPermisoRepository repositorio;
        private final Map<String, Optional<RolVistaPermiso>> porCodigoDeVista = new HashMap<>();
        private boolean rolCompletoCargado;

        PermisosLeidos(Integer rolId, RolVistaPermisoRepository repositorio) {
            this.rolId = rolId;
            this.repositorio = repositorio;
        }

        Optional<RolVistaPermiso> de(String codigoDeVista) {
            Optional<RolVistaPermiso> conocido = porCodigoDeVista.get(codigoDeVista);
            if (conocido != null) {
                return conocido;
            }
            if (rolCompletoCargado) {
                return Optional.empty();
            }
            if (porCodigoDeVista.isEmpty()) {
                Optional<RolVistaPermiso> permiso = repositorio.findByRolIdAndVistaCodigo(rolId, codigoDeVista);
                porCodigoDeVista.put(codigoDeVista, permiso);
                return permiso;
            }
            cargarElRolCompleto();
            return porCodigoDeVista.getOrDefault(codigoDeVista, Optional.empty());
        }

        private void cargarElRolCompleto() {
            repositorio.findByRolIdWithVistaAndVentana(rolId)
                    .forEach(permiso -> porCodigoDeVista.put(permiso.getVista().getCodigo(), Optional.of(permiso)));
            rolCompletoCargado = true;
        }
    }
}
