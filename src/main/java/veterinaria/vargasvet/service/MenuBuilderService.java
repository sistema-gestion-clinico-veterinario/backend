package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.RolVentanaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.UsuarioPorRolPermiso;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.VentanaRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuBuilderService {

    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDTO> construirMenu(Integer usuarioId, String rolActivo) {
        List<UsuarioPorRol> asignaciones = usuarioPorRolRepository.findByUsuarioId(usuarioId);

        Map<String, UsuarioPorRolPermiso> permisosPorVentana = new HashMap<>();

        for (UsuarioPorRol upr : asignaciones) {
            boolean esRolActivo = rolActivo == null || upr.getRol().getName().equals(rolActivo);
            if (!esRolActivo) continue;

            if (upr.getPermisos() == null) continue;
            for (UsuarioPorRolPermiso permiso : upr.getPermisos()) {
                String codigo = permiso.getVentana().getCodigo();
                permisosPorVentana.merge(codigo, permiso, (existing, nuevo) ->
                        mergePermisos(existing, nuevo));
            }
        }

        if (permisosPorVentana.isEmpty()) {
            for (UsuarioPorRol upr : asignaciones) {
                boolean esRolActivo = rolActivo == null || upr.getRol().getName().equals(rolActivo);
                if (!esRolActivo) continue;
                List<RolVentanaPermiso> rolPermisos = rolVentanaPermisoRepository.findByRolId(upr.getRol().getId());
                for (RolVentanaPermiso rp : rolPermisos) {
                    UsuarioPorRolPermiso synthetic = new UsuarioPorRolPermiso();
                    synthetic.setVentana(rp.getVentana());
                    synthetic.setLeer(rp.isLeer());
                    synthetic.setEscribir(rp.isEscribir());
                    synthetic.setModificar(rp.isModificar());
                    synthetic.setEliminar(rp.isEliminar());
                    permisosPorVentana.put(rp.getVentana().getCodigo(), synthetic);
                }
            }
        }

        if (permisosPorVentana.isEmpty()) return Collections.emptyList();

        List<Ventana> raices = ventanaRepository.findRaices();

        return raices.stream()
                .filter(v -> tieneAccesoEnArbol(v, permisosPorVentana))
                .map(v -> toDTO(v, permisosPorVentana, rolActivo))
                .collect(Collectors.toList());
    }

    private MenuItemDTO toDTO(Ventana ventana, Map<String, UsuarioPorRolPermiso> permisos, String rolActivo) {
        return toDTO(ventana, permisos, rolActivo, new HashSet<>());
    }

    private MenuItemDTO toDTO(Ventana ventana, Map<String, UsuarioPorRolPermiso> permisos, String rolActivo, Set<String> visitados) {
        if (ventana == null || !visitados.add(ventana.getCodigo())) return null;
        UsuarioPorRolPermiso permiso = permisos.get(ventana.getCodigo());

        List<MenuItemDTO> hijos = Collections.emptyList();
        if (ventana.getHijos() != null && !ventana.getHijos().isEmpty()) {
            hijos = ventana.getHijos().stream()
                    .filter(h -> h.isActivo() && tieneAccesoEnArbol(h, permisos))
                    .map(h -> toDTO(h, permisos, rolActivo, new HashSet<>(visitados)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        String ruta = construirRuta(ventana.getRuta(), rolActivo);

        return MenuItemDTO.builder()
                .id(ventana.getId())
                .codigo(ventana.getCodigo())
                .nombre(ventana.getNombre())
                .icono(ventana.getIcono())
                .ruta(ruta)
                .hijos(hijos.isEmpty() ? null : hijos)
                .vistas(ventana.getVistas() != null ? ventana.getVistas().stream()
                        .filter(Vista::isActivo)
                        .map(v -> VistaDTO.builder()
                                .id(v.getId())
                                .codigo(v.getCodigo())
                                .nombre(v.getNombre())
                                .ruta(v.getRuta())
                                .activo(v.isActivo())
                                .build())
                        .collect(Collectors.toList()) : null)
                .leer(permiso != null && permiso.isLeer())
                .escribir(permiso != null && permiso.isEscribir())
                .modificar(permiso != null && permiso.isModificar())
                .eliminar(permiso != null && permiso.isEliminar())
                .build();
    }

    private boolean tieneAccesoEnArbol(Ventana ventana, Map<String, UsuarioPorRolPermiso> permisos) {
        return tieneAccesoEnArbol(ventana, permisos, new HashSet<>());
    }

    private boolean tieneAccesoEnArbol(Ventana ventana, Map<String, UsuarioPorRolPermiso> permisos, Set<String> visitados) {
        if (ventana == null || !visitados.add(ventana.getCodigo())) return false;
        UsuarioPorRolPermiso permiso = permisos.get(ventana.getCodigo());
        if (permiso != null && permiso.isLeer()) return true;

        if (ventana.getHijos() != null) {
            return ventana.getHijos().stream()
                    .filter(Ventana::isActivo)
                    .anyMatch(h -> tieneAccesoEnArbol(h, permisos, visitados));
        }
        return false;
    }

    private String construirRuta(String rutaBase, String rolActivo) {
        if (rutaBase == null || rutaBase.isBlank()) return null;

        if (rolActivo != null) {
            String prefijo;
            if (rolActivo.contains("SUPER_ADMIN") || rolActivo.contains("ADMIN")) {
                prefijo = "/admin";
            } else if (rolActivo.contains("CLIENTE") || rolActivo.contains("APODERADO")) {
                prefijo = "/apoderado";
            } else {
                prefijo = "/empleado";
            }

            // Normalizar rutaBase para que inicie con '/'
            String ruta = rutaBase.startsWith("/") ? rutaBase : "/" + rutaBase;

            // Si la ruta ya tiene el prefijo adecuado, retornar tal cual
            if (ruta.startsWith(prefijo + "/")) {
                return ruta;
            }

            // Si la ruta ya tiene otro prefijo de rol válido, retornar tal cual para evitar colisiones
            if (ruta.startsWith("/admin/") || ruta.startsWith("/empleado/") || ruta.startsWith("/apoderado/")) {
                return ruta;
            }

            return prefijo + ruta;
        }
        return rutaBase.startsWith("/") ? rutaBase : "/" + rutaBase;
    }

    private UsuarioPorRolPermiso mergePermisos(UsuarioPorRolPermiso a, UsuarioPorRolPermiso b) {
        a.setLeer(a.isLeer() || b.isLeer());
        a.setEscribir(a.isEscribir() || b.isEscribir());
        a.setModificar(a.isModificar() || b.isModificar());
        a.setEliminar(a.isEliminar() || b.isEliminar());
        return a;
    }
}
