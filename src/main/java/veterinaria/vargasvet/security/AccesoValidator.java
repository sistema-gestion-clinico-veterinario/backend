package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.UsuarioPorRolPermiso;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class AccesoValidator {

    private final UsuarioPorRolPermisoRepository permisoRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final UsuarioRepository usuarioRepository;

    public void validarLeer(String codigoVista) {
        if (!tienePermiso(codigoVista, UsuarioPorRolPermiso::isLeer, RolVistaPermiso::isLeer))
            throw new AccessDeniedException("Sin acceso de lectura a: " + codigoVista);
    }

    public void validarEscribir(String codigoVista) {
        if (!tienePermiso(codigoVista, UsuarioPorRolPermiso::isEscribir, RolVistaPermiso::isEscribir))
            throw new AccessDeniedException("Sin acceso de escritura a: " + codigoVista);
    }

    public void validarModificar(String codigoVista) {
        if (!tienePermiso(codigoVista, UsuarioPorRolPermiso::isModificar, RolVistaPermiso::isModificar))
            throw new AccessDeniedException("Sin acceso de modificación a: " + codigoVista);
    }

    public void validarEliminar(String codigoVista) {
        if (!tienePermiso(codigoVista, UsuarioPorRolPermiso::isEliminar, RolVistaPermiso::isEliminar))
            throw new AccessDeniedException("Sin acceso de eliminación a: " + codigoVista);
    }

    public boolean puedeLeer(String codigoVista) {
        if (SecurityUtils.isSuperAdmin()) return true;
        return tienePermiso(codigoVista, UsuarioPorRolPermiso::isLeer, RolVistaPermiso::isLeer);
    }

    private boolean tienePermiso(String codigoVista,
                                  Predicate<UsuarioPorRolPermiso> usuarioCheck,
                                  Predicate<RolVistaPermiso> rolCheck) {
        if (SecurityUtils.isSuperAdmin()) return true;

        Integer usuarioId = resolverUsuarioId();

        List<UsuarioPorRolPermiso> userPermisos = permisoRepository
                .findByUsuarioIdAndVistaCodigo(usuarioId, codigoVista);
        if (userPermisos.stream().anyMatch(usuarioCheck)) return true;

        List<UsuarioPorRol> asignaciones = usuarioPorRolRepository.findByUsuarioId(usuarioId);
        for (UsuarioPorRol upr : asignaciones) {
            List<RolVistaPermiso> rolPermisos = rolVistaPermisoRepository.findByRolId(upr.getRol().getId());
            boolean tiene = rolPermisos.stream()
                    .filter(rp -> rp.getVista().getCodigo().equals(codigoVista))
                    .anyMatch(rolCheck);
            if (tiene) return true;
        }
        return false;
    }

    private Integer resolverUsuarioId() {
        String email = SecurityUtils.getCurrentUserEmail();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"))
                .getId();
    }
}
