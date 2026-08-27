package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;

import java.util.List;
import java.util.Objects;
import veterinaria.vargasvet.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class UsuarioPorRolService {

    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final UsuarioPorRolPermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final VistaRepository vistaRepository;

    @Transactional(readOnly = true)
    public List<UsuarioPorRol> listarPorUsuario(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        assertSameTenant(usuario);
        return usuarioPorRolRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public UsuarioPorRol asignarRol(Integer usuarioId, Integer rolId) {
        if (usuarioPorRolRepository.existsByUsuarioIdAndRolId(usuarioId, rolId)) {
            throw new IllegalArgumentException("El usuario ya tiene asignado ese rol");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Role rol = roleRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertSameTenant(usuario);
        assertAssignableRole(rol);

        UsuarioPorRol upr = new UsuarioPorRol();
        upr.setUsuario(usuario);
        upr.setRol(rol);
        return usuarioPorRolRepository.save(upr);
    }

    @Transactional
    public UsuarioPorRolPermiso asignarPermiso(Integer usuarioPorRolId, Integer vistaId,
                                                boolean leer, boolean escribir,
                                                boolean modificar, boolean eliminar) {
        UsuarioPorRol upr = usuarioPorRolRepository.findById(usuarioPorRolId)
                .orElseThrow(() -> new ResourceNotFoundException("AsignacionRol no encontrada"));
        assertSameTenant(upr.getUsuario());

        if (!upr.getUsuario().isActivo()) {
            throw new IllegalArgumentException("No se puede asignar permisos a un usuario inactivo");
        }

        Vista vista = vistaRepository.findById(vistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada"));

        UsuarioPorRolPermiso permiso = permisoRepository
                .findByUsuarioPorRolIdAndVistaCodigo(usuarioPorRolId, vista.getCodigo())
                .orElse(new UsuarioPorRolPermiso());

        permiso.setUsuarioPorRol(upr);
        permiso.setVista(vista);
        permiso.setLeer(leer);
        permiso.setEscribir(escribir);
        permiso.setModificar(modificar);
        permiso.setEliminar(eliminar);

        return permisoRepository.save(permiso);
    }

    @Transactional
    public void revocarRol(Integer usuarioPorRolId) {
        UsuarioPorRol upr = usuarioPorRolRepository.findById(usuarioPorRolId)
                .orElseThrow(() -> new ResourceNotFoundException("AsignacionRol no encontrada"));
        assertSameTenant(upr.getUsuario());
        if (!SecurityUtils.isSuperAdmin() && "ROLE_SUPER_ADMIN".equals(upr.getRol().getName())) {
            throw new AccessDeniedException("No puede revocar este rol");
        }
        usuarioPorRolRepository.delete(upr);
    }

    private void assertSameTenant(Usuario usuario) {
        if (SecurityUtils.isSuperAdmin()) return;
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        Integer targetCompanyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        if (currentCompanyId == null || !Objects.equals(currentCompanyId, targetCompanyId)) {
            throw new AccessDeniedException("No tiene permisos para gestionar este usuario");
        }
    }

    private void assertAssignableRole(Role role) {
        if (SecurityUtils.isSuperAdmin()) return;
        if ("ROLE_SUPER_ADMIN".equals(role.getName())) {
            throw new AccessDeniedException("No puede asignar este rol");
        }
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (roleCompanyId != null && !Objects.equals(roleCompanyId, currentCompanyId)) {
            throw new AccessDeniedException("No puede asignar un rol de otra empresa");
        }
    }
}
