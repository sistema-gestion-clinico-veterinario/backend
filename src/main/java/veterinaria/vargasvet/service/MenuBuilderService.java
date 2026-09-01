package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.*;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.MenuStructureDTO;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RolVentanaConfiguracionRepository;
import veterinaria.vargasvet.repository.RolVistaConfiguracionRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.domain.enums.DataScope;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuBuilderService {

    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final RolVentanaConfiguracionRepository rolVentanaConfiguracionRepository;
    private final RolVistaConfiguracionRepository rolVistaConfiguracionRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDTO> construirMenu(Integer usuarioId, Integer roleId) {
        EffectiveRoleAccess acceso = obtenerAccesoRol(usuarioId, roleId);
        Map<String, UsuarioPorRolPermiso> permisosPorVista = acceso.permisos();
        if (permisosPorVista.isEmpty()) return Collections.emptyList();

        return permisosPorVista.values().stream()
                .filter(p -> p.isLeer() && p.getVista() != null && p.getVista().isActivo()
                        && p.getVista().isVisibleMenu())
                .map(p -> toDTO(p.getVista(), p, acceso.configuracionesVista().get(p.getVista().getId()),
                        acceso.dataScopes().getOrDefault(p.getVista().getId(), DataScope.OWN)))
                .sorted(Comparator.comparingInt(MenuItemDTO::getOrden))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuStructureDTO> construirMenuJerarquico(Integer usuarioId, Integer roleId) {
        EffectiveRoleAccess acceso = obtenerAccesoRol(usuarioId, roleId);
        Map<String, UsuarioPorRolPermiso> permisosPorVista = acceso.permisos();
        if (permisosPorVista.isEmpty()) return Collections.emptyList();

        Map<Integer, List<MenuItemDTO>> vistasPorVentana = new LinkedHashMap<>();
        Map<Integer, Ventana> ventanas = new LinkedHashMap<>();
        List<MenuItemDTO> vistasSueltas = new ArrayList<>();

        for (UsuarioPorRolPermiso permiso : permisosPorVista.values()) {
            Vista vista = permiso.getVista();
            if (!permiso.isLeer() || vista == null || !vista.isActivo() || !vista.isVisibleMenu()
                    || !isAudienceCompatible(acceso.role().getScope(), vista.getAudience())) {
                continue;
            }

            MenuItemDTO dto = toDTO(vista, permiso, acceso.configuracionesVista().get(vista.getId()),
                    acceso.dataScopes().getOrDefault(vista.getId(), DataScope.OWN));
            Ventana ventana = vista.getVentana();
            if (ventana != null && ventana.isActivo()) {
                ventanas.putIfAbsent(ventana.getId(), ventana);
                vistasPorVentana.computeIfAbsent(ventana.getId(), ignored -> new ArrayList<>()).add(dto);
            } else {
                vistasSueltas.add(dto);
            }
        }

        List<MenuStructureDTO> resultado = new ArrayList<>();
        Map<Integer, RolVentanaConfiguracion> configuraciones = acceso.configuraciones();

        vistasPorVentana.forEach((ventanaId, vistas) -> {
            Ventana ventana = ventanas.get(ventanaId);
            RolVentanaConfiguracion configuracion = configuraciones.get(ventanaId);
            MenuPresentation presentacion = configuracion != null
                    ? configuracion.getPresentacion()
                    : ventana.getPresentacionDefault();
            int ordenVentana = configuracion != null ? configuracion.getOrden() : ventana.getOrden();
            vistas.sort(Comparator.comparingInt(MenuItemDTO::getOrden));

            if (presentacion == MenuPresentation.FLAT) {
                for (MenuItemDTO vista : vistas) {
                    resultado.add(MenuStructureDTO.builder()
                            .ventanaId(ventana.getId())
                            .ventanaCodigo(ventana.getCodigo())
                            .ventanaNombre(ventana.getNombre())
                            .ventanaIcono(ventana.getIcono())
                            .presentacion(MenuPresentation.FLAT)
                            .grupo(null)
                            .orden(combineOrder(ordenVentana, vista.getOrden()))
                            .vistas(Collections.singletonList(vista))
                            .build());
                }
            } else {
                resultado.add(MenuStructureDTO.builder()
                        .ventanaId(ventana.getId())
                        .ventanaCodigo(ventana.getCodigo())
                        .ventanaNombre(ventana.getNombre())
                        .ventanaIcono(ventana.getIcono())
                        .presentacion(MenuPresentation.GROUPED)
                        .grupo(null)
                        .orden(combineOrder(ordenVentana, 0))
                        .vistas(vistas)
                        .build());
            }
        });

        for (MenuItemDTO vista : vistasSueltas) {
            resultado.add(MenuStructureDTO.builder()
                    .ventanaId(null)
                    .ventanaNombre(vista.getNombre())
                    .presentacion(MenuPresentation.FLAT)
                    .grupo(null)
                    .orden(combineOrder(vista.getOrden(), 0))
                    .vistas(Collections.singletonList(vista))
                    .build());
        }

        resultado.sort(Comparator.comparingInt(MenuStructureDTO::getOrden));
        return resultado;
    }

    private EffectiveRoleAccess obtenerAccesoRol(Integer usuarioId, Integer roleId) {
        if (roleId == null) {
            return EffectiveRoleAccess.empty();
        }
        UsuarioPorRol asignacion = usuarioPorRolRepository
                .findActiveAssignmentByUsuarioIdAndRoleId(usuarioId, roleId)
                .orElse(null);
        if (asignacion == null) {
            return EffectiveRoleAccess.empty();
        }

        Map<String, UsuarioPorRolPermiso> permisosPorVista = new HashMap<>();
        Map<Integer, DataScope> dataScopes = new HashMap<>();

        List<RolVistaPermiso> rolPermisos = rolVistaPermisoRepository
                .findByRolIdWithVistaAndVentana(asignacion.getRol().getId());
        for (RolVistaPermiso rp : rolPermisos) {
            if (rp.getVista() == null) {
                continue;
            }
            UsuarioPorRolPermiso synthetic = new UsuarioPorRolPermiso();
            synthetic.setVista(rp.getVista());
            synthetic.setLeer(rp.isLeer());
            synthetic.setEscribir(rp.isEscribir());
            synthetic.setModificar(rp.isModificar());
            synthetic.setEliminar(rp.isEliminar());
            permisosPorVista.put(rp.getVista().getCodigo(), synthetic);
            boolean administrativeScope = asignacion.getRol().getPurpose()
                    == veterinaria.vargasvet.domain.enums.RolePurpose.PLATFORM_ADMIN
                    || asignacion.getRol().getPurpose()
                    == veterinaria.vargasvet.domain.enums.RolePurpose.COMPANY_ADMIN;
            dataScopes.put(rp.getVista().getId(), administrativeScope
                    ? DataScope.COMPANY
                    : (rp.getDataScope() != null ? rp.getDataScope() : DataScope.OWN));
        }

        Map<Integer, RolVentanaConfiguracion> configuraciones = rolVentanaConfiguracionRepository
                .findByRolIdWithVentana(asignacion.getRol().getId()).stream()
                .collect(Collectors.toMap(
                        config -> config.getVentana().getId(),
                        config -> config,
                        (existing, replacement) -> existing
                ));
        Map<Integer, RolVistaConfiguracion> configuracionesVista = rolVistaConfiguracionRepository
                .findByRolIdWithVistaAndVentana(asignacion.getRol().getId()).stream()
                .collect(Collectors.toMap(
                        config -> config.getVista().getId(),
                        config -> config,
                        (existing, replacement) -> existing
                ));
        return new EffectiveRoleAccess(
                asignacion.getRol(), permisosPorVista, configuraciones, configuracionesVista, dataScopes);
    }

    private MenuItemDTO toDTO(
            Vista vista,
            UsuarioPorRolPermiso permiso,
            RolVistaConfiguracion configuracion,
            DataScope dataScope) {
        return MenuItemDTO.builder()
                .id(vista.getId())
                .codigo(vista.getCodigo())
                .nombre(vista.getNombre())
                .ruta(vista.getRuta())
                .grupo(vista.getGrupo())
                .orden(configuracion != null ? configuracion.getOrden() : vista.getOrden())
                .ordenGrupo(vista.getOrdenGrupo())
                .activo(vista.isActivo())
                .icono(vista.getIcono())
                .leer(permiso != null && permiso.isLeer())
                .escribir(permiso != null && permiso.isEscribir())
                .modificar(permiso != null && permiso.isModificar())
                .eliminar(permiso != null && permiso.isEliminar())
                .dataScope(dataScope)
                .build();
    }


    private boolean isAudienceCompatible(RoleScope scope, ViewAudience audience) {
        if (scope == RoleScope.PLATFORM || audience == ViewAudience.SHARED) return true;
        return (scope == RoleScope.STAFF && audience == ViewAudience.STAFF)
                || (scope == RoleScope.CLIENT && audience == ViewAudience.CLIENT);
    }

    private int combineOrder(Integer groupOrder, Integer itemOrder) {
        int group = groupOrder != null ? Math.max(groupOrder, 0) : 0;
        int item = itemOrder != null ? Math.max(itemOrder, 0) : 0;
        return group * 1_000 + Math.min(item, 999);
    }

    @Transactional(readOnly = true)
    public List<String> construirPermissions(Integer usuarioId, Integer roleId) {
        Map<String, UsuarioPorRolPermiso> permisos = obtenerAccesoRol(usuarioId, roleId).permisos();
        Set<String> resultado = new LinkedHashSet<>();

        for (Map.Entry<String, UsuarioPorRolPermiso> entry : permisos.entrySet()) {
            String codigo = entry.getKey();
            UsuarioPorRolPermiso p = entry.getValue();

            if (p.isLeer()) resultado.add(codigo + ":LEER");
            if (p.isEscribir()) resultado.add(codigo + ":ESCRIBIR");
            if (p.isModificar()) resultado.add(codigo + ":MODIFICAR");
            if (p.isEliminar()) resultado.add(codigo + ":ELIMINAR");
        }

        return new ArrayList<>(resultado);
    }

    private record EffectiveRoleAccess(
            Role role,
            Map<String, UsuarioPorRolPermiso> permisos,
            Map<Integer, RolVentanaConfiguracion> configuraciones,
            Map<Integer, RolVistaConfiguracion> configuracionesVista,
            Map<Integer, DataScope> dataScopes) {

        private static EffectiveRoleAccess empty() {
            Role emptyRole = new Role();
            emptyRole.setScope(RoleScope.STAFF);
            return new EffectiveRoleAccess(
                    emptyRole, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap());
        }
    }
}
