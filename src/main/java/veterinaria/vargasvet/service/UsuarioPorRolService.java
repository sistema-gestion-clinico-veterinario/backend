package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.*;

import java.util.List;

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
        usuarioPorRolRepository.delete(upr);
    }
}
