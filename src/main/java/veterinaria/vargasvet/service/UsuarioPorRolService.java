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
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;

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
        assertAssignableRole(usuario, rol);

        UsuarioPorRol upr = new UsuarioPorRol();
        upr.setUsuario(usuario);
        upr.setRol(rol);
        return usuarioPorRolRepository.save(upr);
    }

    @Transactional
    public void revocarRol(Integer usuarioPorRolId) {
        UsuarioPorRol upr = usuarioPorRolRepository.findById(usuarioPorRolId)
                .orElseThrow(() -> new ResourceNotFoundException("AsignacionRol no encontrada"));
        assertSameTenant(upr.getUsuario());
        if (!SecurityUtils.isSuperAdmin() && upr.getRol().getScope() == veterinaria.vargasvet.domain.enums.RoleScope.PLATFORM) {
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

    private void assertAssignableRole(Usuario usuario, Role role) {
        if (SecurityUtils.isSuperAdmin()) return;
        if (role.getScope() == veterinaria.vargasvet.domain.enums.RoleScope.PLATFORM
                || role.getCompany() == null) {
            throw new AccessDeniedException("No puede asignar este rol");
        }
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (roleCompanyId != null && !Objects.equals(roleCompanyId, currentCompanyId)) {
            throw new AccessDeniedException("No puede asignar un rol de otra empresa");
        }
        if (role.getScope() == veterinaria.vargasvet.domain.enums.RoleScope.CLIENT
                && usuario.getApoderado() == null) {
            throw new AccessDeniedException("Un rol de cliente solo puede asignarse a un apoderado");
        }
        if (role.getScope() == veterinaria.vargasvet.domain.enums.RoleScope.STAFF
                && usuario.getEmpleado() == null) {
            throw new AccessDeniedException("Un rol de personal solo puede asignarse a un empleado");
        }
    }
}
