package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.RolVentanaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.UsuarioPorRolPermiso;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class AccesoValidator {

    private final UsuarioPorRolPermisoRepository permisoRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final UsuarioRepository usuarioRepository;

    public void validarLeer(String codigoVentana) {
        if (!tienePermiso(codigoVentana, UsuarioPorRolPermiso::isLeer, RolVentanaPermiso::isLeer))
            throw new AccessDeniedException("Sin acceso de lectura a: " + codigoVentana);
    }

    public void validarEscribir(String codigoVentana) {
        if (!tienePermiso(codigoVentana, UsuarioPorRolPermiso::isEscribir, RolVentanaPermiso::isEscribir))
            throw new AccessDeniedException("Sin acceso de escritura a: " + codigoVentana);
    }

    public void validarModificar(String codigoVentana) {
        if (!tienePermiso(codigoVentana, UsuarioPorRolPermiso::isModificar, RolVentanaPermiso::isModificar))
            throw new AccessDeniedException("Sin acceso de modificación a: " + codigoVentana);
    }

    public void validarEliminar(String codigoVentana) {
        if (!tienePermiso(codigoVentana, UsuarioPorRolPermiso::isEliminar, RolVentanaPermiso::isEliminar))
            throw new AccessDeniedException("Sin acceso de eliminación a: " + codigoVentana);
    }

    public boolean puedeLeer(String codigoVentana) {
        if (SecurityUtils.isSuperAdmin()) return true;
        return tienePermiso(codigoVentana, UsuarioPorRolPermiso::isLeer, RolVentanaPermiso::isLeer);
    }

    private boolean tienePermiso(String codigoVentana,
                                  Predicate<UsuarioPorRolPermiso> usuarioCheck,
                                  Predicate<RolVentanaPermiso> rolCheck) {
        if (SecurityUtils.isSuperAdmin()) return true;

        Integer usuarioId = resolverUsuarioId();

        List<UsuarioPorRolPermiso> userPermisos = permisoRepository
                .findByUsuarioIdAndVentanaCodigo(usuarioId, codigoVentana);
        if (!userPermisos.isEmpty()) {
            return userPermisos.stream().anyMatch(usuarioCheck);
        }

        List<UsuarioPorRol> asignaciones = usuarioPorRolRepository.findByUsuarioId(usuarioId);
        for (UsuarioPorRol upr : asignaciones) {
            List<RolVentanaPermiso> rolPermisos = rolVentanaPermisoRepository.findByRolId(upr.getRol().getId());
            boolean tiene = rolPermisos.stream()
                    .filter(rp -> rp.getVentana().getCodigo().equals(codigoVentana))
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
