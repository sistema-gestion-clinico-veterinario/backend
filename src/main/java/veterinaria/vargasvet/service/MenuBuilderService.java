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

    // Mapeo de código de Vista → permisos que otorga cada flag
    private static final Map<String, String[]> LEER     = new HashMap<>();
    private static final Map<String, String[]> ESCRIBIR = new HashMap<>();
    private static final Map<String, String[]> MODIFICAR= new HashMap<>();
    private static final Map<String, String[]> ELIMINAR = new HashMap<>();

    static {
        LEER.put("VISTA_CLIENTES",      new String[]{"APODERADO_READ"});
        ESCRIBIR.put("VISTA_CLIENTES",  new String[]{"APODERADO_CREATE"});
        MODIFICAR.put("VISTA_CLIENTES", new String[]{"APODERADO_UPDATE", "APODERADO_STATUS"});
        ELIMINAR.put("VISTA_CLIENTES",  new String[]{"APODERADO_DELETE"});

        LEER.put("VISTA_MASCOTAS",      new String[]{"PET_READ"});
        ESCRIBIR.put("VISTA_MASCOTAS",  new String[]{"PET_CREATE", "PET_WRITE"});
        MODIFICAR.put("VISTA_MASCOTAS", new String[]{"PET_UPDATE", "PET_STATUS"});
        ELIMINAR.put("VISTA_MASCOTAS",  new String[]{"PET_DELETE"});

        LEER.put("VISTA_HISTORIAS",      new String[]{"CLINICAL_RECORD_READ", "PET_HISTORY_READ"});
        MODIFICAR.put("VISTA_HISTORIAS", new String[]{"CLINICAL_RECORD_MANAGE", "PET_HISTORY_WRITE"});

        LEER.put("VISTA_RECETAS",        new String[]{"PET_HISTORY_READ"});
        MODIFICAR.put("VISTA_RECETAS",   new String[]{"PET_HISTORY_WRITE"});

        LEER.put("VISTA_CITAS_AGENDA",      new String[]{"CITA_READ"});
        ESCRIBIR.put("VISTA_CITAS_AGENDA",  new String[]{"CITA_CREATE"});
        MODIFICAR.put("VISTA_CITAS_AGENDA", new String[]{"CITA_UPDATE", "CITA_INICIAR"});
        ELIMINAR.put("VISTA_CITAS_AGENDA",  new String[]{"CITA_DELETE", "CITA_CANCEL"});

        LEER.put("VISTA_EMPLEADOS",      new String[]{"EMPLEADO_READ"});
        ESCRIBIR.put("VISTA_EMPLEADOS",  new String[]{"EMPLEADO_CREATE"});
        MODIFICAR.put("VISTA_EMPLEADOS", new String[]{"EMPLEADO_UPDATE", "EMPLEADO_STATUS"});
        ELIMINAR.put("VISTA_EMPLEADOS",  new String[]{"EMPLEADO_DELETE"});

        LEER.put("VISTA_HORARIOS",      new String[]{"HORARIO_READ"});
        MODIFICAR.put("VISTA_HORARIOS", new String[]{"HORARIO_MANAGE"});

        LEER.put("VISTA_MI_HORARIO",    new String[]{"USER_READ"});

        LEER.put("VISTA_COMPLEMENTARIO",      new String[]{"TIPO_EMPLEADO_READ", "ESPECIALIDAD_READ", "SERVICIO_READ"});
        ESCRIBIR.put("VISTA_COMPLEMENTARIO",  new String[]{"TIPO_EMPLEADO_CREATE", "ESPECIALIDAD_CREATE", "SERVICIO_CREATE"});
        MODIFICAR.put("VISTA_COMPLEMENTARIO", new String[]{"TIPO_EMPLEADO_UPDATE", "TIPO_EMPLEADO_STATUS", "ESPECIALIDAD_UPDATE", "SERVICIO_UPDATE", "SERVICIO_TOGGLE"});
        ELIMINAR.put("VISTA_COMPLEMENTARIO",  new String[]{"TIPO_EMPLEADO_DELETE", "ESPECIALIDAD_DELETE", "SERVICIO_DELETE"});

        LEER.put("VISTA_COMPANY",      new String[]{"COMPANY_READ"});
        MODIFICAR.put("VISTA_COMPANY", new String[]{"COMPANY_UPDATE", "COMPANY_MANAGE"});

        LEER.put("VISTA_ROLES",      new String[]{"ROLE_MANAGE"});
        ESCRIBIR.put("VISTA_ROLES",  new String[]{"ROLE_MANAGE"});
        MODIFICAR.put("VISTA_ROLES", new String[]{"ROLE_MANAGE"});
        ELIMINAR.put("VISTA_ROLES",  new String[]{"ROLE_MANAGE"});

        LEER.put("VISTA_PAGOS",      new String[]{"SALE_READ"});
        MODIFICAR.put("VISTA_PAGOS", new String[]{"SALE_MANAGE"});

        LEER.put("VISTA_DASHBOARD",          new String[]{"ADMIN_DASHBOARD"});
        LEER.put("VISTA_APODERADO_DASHBOARD",new String[]{"APODERADO_DASHBOARD"});

        LEER.put("VISTA_AUDITORIA_ADMIN",    new String[]{"COMPANY_MANAGE"});
        LEER.put("VISTA_VENTANAS",           new String[]{"USER_MANAGE"});
    }

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
                UsuarioPorRolPermiso synthetic = new UsuarioPorRolPermiso();
                synthetic.setVista(rp.getVista());
                synthetic.setLeer(rp.isLeer());
                synthetic.setEscribir(rp.isEscribir());
                synthetic.setModificar(rp.isModificar());
                synthetic.setEliminar(rp.isEliminar());
                permisosPorVista.merge(codigo, synthetic, this::mergePermisos);
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

    @Transactional(readOnly = true)
    public List<String> construirPermissions(Integer usuarioId, String rolActivo) {
        Map<String, UsuarioPorRolPermiso> permisos = obtenerPermisosUsuario(usuarioId, rolActivo);
        Set<String> resultado = new LinkedHashSet<>();

        for (Map.Entry<String, UsuarioPorRolPermiso> entry : permisos.entrySet()) {
            String codigo = entry.getKey();
            UsuarioPorRolPermiso p = entry.getValue();

            if (p.isLeer()     && LEER.containsKey(codigo))     Collections.addAll(resultado, LEER.get(codigo));
            if (p.isEscribir() && ESCRIBIR.containsKey(codigo)) Collections.addAll(resultado, ESCRIBIR.get(codigo));
            if (p.isModificar()&& MODIFICAR.containsKey(codigo))Collections.addAll(resultado, MODIFICAR.get(codigo));
            if (p.isEliminar() && ELIMINAR.containsKey(codigo)) Collections.addAll(resultado, ELIMINAR.get(codigo));
        }

        return new ArrayList<>(resultado);
    }
}
