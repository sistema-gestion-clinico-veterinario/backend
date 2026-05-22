package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.MenuStructureDTO;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuBuilderService {

    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final VentanaRepository ventanaRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDTO> construirMenu(Integer usuarioId, String rolActivo) {
        Map<String, UsuarioPorRolPermiso> permisosPorVista = obtenerPermisosUsuario(usuarioId, rolActivo);
        if (permisosPorVista.isEmpty()) return Collections.emptyList();

        return vistaRepository.findAll().stream()
                .filter(v -> v.isActivo() && permisosPorVista.containsKey(v.getCodigo()))
                .map(v -> toDTO(v, permisosPorVista.get(v.getCodigo())))
                .sorted(Comparator.comparingInt(MenuItemDTO::getOrden))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuStructureDTO> construirMenuJerarquico(Integer usuarioId, String rolActivo) {
        Map<String, UsuarioPorRolPermiso> permisosPorVista = obtenerPermisosUsuario(usuarioId, rolActivo);
        if (permisosPorVista.isEmpty()) return Collections.emptyList();

        List<Vista> vistasAccesibles = vistaRepository.findAll().stream()
                .filter(v -> v.isActivo() && permisosPorVista.containsKey(v.getCodigo()))
                .collect(Collectors.toList());

        Map<Ventana, List<MenuItemDTO>> porVentana = new LinkedHashMap<>();
        List<MenuItemDTO> vistasSueltas = new ArrayList<>();

        for (Vista vista : vistasAccesibles) {
            MenuItemDTO dto = toDTO(vista, permisosPorVista.get(vista.getCodigo()));
            if (vista.getVentana() != null) {
                porVentana.computeIfAbsent(vista.getVentana(), k -> new ArrayList<>()).add(dto);
            } else {
                vistasSueltas.add(dto);
            }
        }

        List<MenuStructureDTO> resultado = new ArrayList<>();

        porVentana.forEach((ventana, vistas) -> {
            vistas.sort(Comparator.comparingInt(MenuItemDTO::getOrden));
            resultado.add(MenuStructureDTO.builder()
                    .ventanaId(ventana.getId())
                    .ventanaCodigo(ventana.getCodigo())
                    .ventanaNombre(ventana.getNombre())
                    .grupo(ventana.getGrupo())
                    .orden(ventana.getOrden())
                    .vistas(vistas)
                    .build());
        });

        resultado.sort(Comparator.comparingInt(MenuStructureDTO::getOrden));

        for (MenuItemDTO vista : vistasSueltas) {
            resultado.add(MenuStructureDTO.builder()
                    .ventanaId(null)
                    .ventanaNombre(vista.getNombre())
                    .orden(vista.getOrden())
                    .vistas(Collections.singletonList(vista))
                    .build());
        }

        return resultado;
    }

    private Map<String, UsuarioPorRolPermiso> obtenerPermisosUsuario(Integer usuarioId, String rolActivo) {
        List<UsuarioPorRol> asignaciones = usuarioPorRolRepository.findByUsuarioId(usuarioId);
        Map<String, UsuarioPorRolPermiso> permisosPorVista = new HashMap<>();

        for (UsuarioPorRol upr : asignaciones) {
            boolean esRolActivo = rolActivo == null || upr.getRol().getName().equals(rolActivo);
            if (!esRolActivo) continue;

            if (upr.getPermisos() != null) {
                for (UsuarioPorRolPermiso permiso : upr.getPermisos()) {
                    String codigo = permiso.getVista().getCodigo();
                    permisosPorVista.merge(codigo, permiso, this::mergePermisos);
                }
            }

            List<RolVistaPermiso> rolPermisos = rolVistaPermisoRepository.findByRolId(upr.getRol().getId());
            for (RolVistaPermiso rp : rolPermisos) {
                String codigo = rp.getVista().getCodigo();
                if (!permisosPorVista.containsKey(codigo)) {
                    UsuarioPorRolPermiso synthetic = new UsuarioPorRolPermiso();
                    synthetic.setVista(rp.getVista());
                    synthetic.setLeer(rp.isLeer());
                    synthetic.setEscribir(rp.isEscribir());
                    synthetic.setModificar(rp.isModificar());
                    synthetic.setEliminar(rp.isEliminar());
                    permisosPorVista.put(codigo, synthetic);
                }
            }
        }

        return permisosPorVista;
    }

    private MenuItemDTO toDTO(Vista vista, UsuarioPorRolPermiso permiso) {
        return MenuItemDTO.builder()
                .id(vista.getId())
                .codigo(vista.getCodigo())
                .nombre(vista.getNombre())
                .ruta(vista.getRuta())
                .grupo(vista.getGrupo())
                .orden(vista.getOrden())
                .activo(vista.isActivo())
                .leer(permiso != null && permiso.isLeer())
                .escribir(permiso != null && permiso.isEscribir())
                .modificar(permiso != null && permiso.isModificar())
                .eliminar(permiso != null && permiso.isEliminar())
                .build();
    }

    private UsuarioPorRolPermiso mergePermisos(UsuarioPorRolPermiso a, UsuarioPorRolPermiso b) {
        a.setLeer(a.isLeer() || b.isLeer());
        a.setEscribir(a.isEscribir() || b.isEscribir());
        a.setModificar(a.isModificar() || b.isModificar());
        a.setEliminar(a.isEliminar() || b.isEliminar());
        return a;
    }
}
