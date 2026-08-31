package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.RolVentanaConfiguracion;
import veterinaria.vargasvet.domain.entity.RolVistaConfiguracion;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.MenuItemType;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.dto.response.RolVentanaConfiguracionDTO;
import veterinaria.vargasvet.dto.response.RolMenuOrdenItemDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.RolVentanaConfiguracionRepository;
import veterinaria.vargasvet.repository.RolVistaConfiguracionRepository;
import veterinaria.vargasvet.service.RoleService;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVentanaConfiguracionRepository rolVentanaConfiguracionRepository;
    private final RolVistaConfiguracionRepository rolVistaConfiguracionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .filter(this::canReadRole)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getRolesByCompany(Integer companyId) {
        assertCompanyAccess(companyId);
        return roleRepository.findByCompanyId(companyId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getAssignableRoles(Integer companyId, RoleScope scope) {
        assertCompanyAccess(companyId);
        if (scope == null || scope == RoleScope.PLATFORM) {
            throw new IllegalArgumentException("El alcance asignable debe ser STAFF o CLIENT");
        }
        java.util.stream.Stream<Role> roles = roleRepository.findByCompanyId(companyId).stream();
        // La plantilla global COMPANY_ADMIN solo es asignable por la plataforma;
        // los roles personalizados continúan aislados por empresa.
        if (scope == RoleScope.STAFF && SecurityUtils.isSuperAdmin()) {
            roles = java.util.stream.Stream.concat(roles,
                    roleRepository.findFirstByCompanyIsNullAndPurpose(RolePurpose.COMPANY_ADMIN).stream());
        }
        return roles
                .filter(Role::isActivo)
                .filter(role -> role.getScope() == scope)
                .distinct()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getSystemRoles() {
        return roleRepository.findByCompanyIsNull().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RolDTO createRole(String nombre, String descripcion, Integer companyId, RoleScope requestedScope) {
        RoleScope scope = requestedScope != null ? requestedScope : RoleScope.STAFF;
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = requireCurrentCompany();
            if (companyId != null && !Objects.equals(companyId, currentCompanyId)) {
                throw new AccessDeniedException("No puede crear roles para otra empresa");
            }
            companyId = currentCompanyId;
            if (scope == RoleScope.PLATFORM) {
                throw new AccessDeniedException("Una empresa no puede crear roles de plataforma");
            }
        }
        nombre =         normalizarNombreRol(nombre);
        descripcion = normalizarDescripcion(descripcion);
        validarDuplicado(nombre, companyId);

        Role role = new Role();
        role.setName(nombre);
        role.setDescripcion(descripcion);
        role.setScope(scope);
        role.setPurpose(RolePurpose.CUSTOM);
        role.setSystemManaged(false);
        role.setProtectedRole(false);

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
            role.setCompany(company);
        }

        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RolDTO updateRole(Integer id, String nombre, String descripcion, RoleScope requestedScope) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        if (role.getPurpose() != RolePurpose.PLATFORM_ADMIN) {
            nombre = normalizarNombreRol(nombre);
            Integer companyId = role.getCompany() != null ? role.getCompany().getId() : null;
            validarDuplicadoEdicion(id, nombre, companyId);
            role.setName(nombre);
            if (requestedScope != null && !role.isSystemManaged()) {
                RoleScope newScope = requestedScope;
                if (!SecurityUtils.isSuperAdmin() && newScope == RoleScope.PLATFORM) {
                    throw new AccessDeniedException("Una empresa no puede convertir un rol en rol de plataforma");
                }
                validateScopeChange(role, newScope);
                role.setScope(newScope);
            }
        }
        role.setDescripcion(normalizarDescripcion(descripcion));

        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RolDTO toggleActivo(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);
        if (role.isProtectedRole()) {
            throw new IllegalArgumentException("No se puede desactivar un rol protegido del sistema");
        }
        role.setActivo(!role.isActivo());
        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        if (role.isProtectedRole()) {
            throw new IllegalArgumentException("No se puede eliminar un rol del sistema");
        }

        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolVistaPermisoDTO> getVistasByRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (!canReadRole(role)) {
            throw new AccessDeniedException("No tiene acceso a este rol");
        }

        List<Vista> todasLasVistas = vistaRepository.findByActivoTrueOrderByNombreAsc();
        Map<Integer, RolVistaPermiso> permisosPorVista = rolVistaPermisoRepository
                .findByRolId(roleId).stream()
                .collect(Collectors.toMap(
                        p -> p.getVista().getId(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return todasLasVistas.stream()
                .filter(v -> isAudienceCompatible(role.getScope(), v.getAudience()))
                .map(v -> {
                    RolVistaPermiso permiso = permisosPorVista.get(v.getId());
                    RolVistaPermisoDTO dto = new RolVistaPermisoDTO();
                    dto.setVistaId(v.getId());
                    dto.setCodigo(v.getCodigo());
                    dto.setNombre(v.getNombre());
                    dto.setRuta(v.getRuta());
                    dto.setGrupo(v.getGrupo());
                    dto.setOrden(v.getOrden());
                    dto.setAudience(v.getAudience());
                    if (v.getVentana() != null) {
                        dto.setVentanaId(v.getVentana().getId());
                        dto.setVentanaCodigo(v.getVentana().getCodigo());
                        dto.setVentanaNombre(v.getVentana().getNombre());
                    }
                    dto.setLeer(permiso != null && permiso.isLeer());
                    dto.setEscribir(permiso != null && permiso.isEscribir());
                    dto.setModificar(permiso != null && permiso.isModificar());
                    dto.setEliminar(permiso != null && permiso.isEliminar());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RolVistaPermisoDTO> saveVistasByRole(Integer roleId, List<RolVistaPermisoDTO> permisos) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        if (!role.isActivo()) {
            throw new IllegalArgumentException("No se pueden asignar permisos a un rol inactivo");
        }

        validarPermisosSolicitados(role, permisos);

        rolVistaPermisoRepository.deleteByRolId(roleId);
        rolVistaPermisoRepository.flush();

        for (RolVistaPermisoDTO dto : permisos) {
            Vista vista = vistaRepository.findById(dto.getVistaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada: " + dto.getVistaId()));
            RolVistaPermiso rvp = new RolVistaPermiso();
            rvp.setRol(role);
            rvp.setVista(vista);
            boolean requiereLectura = dto.isEscribir() || dto.isModificar() || dto.isEliminar();
            rvp.setLeer(dto.isLeer() || requiereLectura);
            rvp.setEscribir(dto.isEscribir());
            rvp.setModificar(dto.isModificar());
            rvp.setEliminar(dto.isEliminar());
            rolVistaPermisoRepository.save(rvp);
        }

        incrementarVersionPermisos(role);

        return getVistasByRole(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolVentanaConfiguracionDTO> getMenuConfiguration(Integer roleId) {
        Role role = requireReadableRole(roleId);
        Map<Integer, RolVentanaConfiguracion> configuraciones = rolVentanaConfiguracionRepository
                .findByRolIdWithVentana(roleId).stream()
                .collect(Collectors.toMap(
                        config -> config.getVentana().getId(),
                        config -> config,
                        (existing, replacement) -> existing
                ));

        Map<Integer, Ventana> ventanasLegibles = rolVistaPermisoRepository
                .findByRolIdWithVistaAndVentana(roleId).stream()
                .filter(RolVistaPermiso::isLeer)
                .map(RolVistaPermiso::getVista)
                .filter(vista -> vista.isActivo() && vista.getVentana() != null && vista.getVentana().isActivo())
                .filter(vista -> isAudienceCompatible(role.getScope(), vista.getAudience()))
                .map(Vista::getVentana)
                .collect(Collectors.toMap(
                        Ventana::getId,
                        ventana -> ventana,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return ventanasLegibles.values().stream()
                .map(ventana -> toMenuConfigurationDTO(ventana, configuraciones.get(ventana.getId())))
                .sorted(Comparator.comparing(RolVentanaConfiguracionDTO::getOrden)
                        .thenComparing(RolVentanaConfiguracionDTO::getNombre))
                .toList();
    }

    @Override
    @Transactional
    public List<RolVentanaConfiguracionDTO> saveMenuConfiguration(
            Integer roleId,
            List<RolVentanaConfiguracionDTO> configuraciones) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);
        if (!role.isActivo()) {
            throw new IllegalArgumentException("No se puede configurar el menú de un rol inactivo");
        }

        List<RolVentanaConfiguracionDTO> solicitudes = configuraciones != null ? configuraciones : List.of();
        Set<Integer> ventanasPermitidas = rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(roleId).stream()
                .filter(RolVistaPermiso::isLeer)
                .map(RolVistaPermiso::getVista)
                .filter(vista -> vista.getVentana() != null)
                .filter(vista -> isAudienceCompatible(role.getScope(), vista.getAudience()))
                .map(vista -> vista.getVentana().getId())
                .collect(Collectors.toSet());

        Set<Integer> idsRecibidos = new HashSet<>();
        for (RolVentanaConfiguracionDTO dto : solicitudes) {
            if (dto.getVentanaId() == null || !idsRecibidos.add(dto.getVentanaId())) {
                throw new IllegalArgumentException("La configuración contiene módulos duplicados o inválidos");
            }
            if (!ventanasPermitidas.contains(dto.getVentanaId())) {
                throw new IllegalArgumentException("El rol no tiene vistas legibles en el módulo indicado");
            }
            if (dto.getOrden() != null && dto.getOrden() < 0) {
                throw new IllegalArgumentException("El orden del módulo no puede ser negativo");
            }
        }

        rolVentanaConfiguracionRepository.deleteByRolId(roleId);
        rolVentanaConfiguracionRepository.flush();

        for (RolVentanaConfiguracionDTO dto : solicitudes) {
            Ventana ventana = ventanaRepository.findById(dto.getVentanaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado"));
            RolVentanaConfiguracion config = new RolVentanaConfiguracion();
            config.setRol(role);
            config.setVentana(ventana);
            config.setPresentacion(dto.getPresentacion() != null
                    ? dto.getPresentacion()
                    : ventana.getPresentacionDefault());
            config.setOrden(dto.getOrden() != null ? dto.getOrden() : ventana.getOrden());
            rolVentanaConfiguracionRepository.save(config);
        }

        incrementarVersionPermisos(role);
        return getMenuConfiguration(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolMenuOrdenItemDTO> getMenuOrder(Integer roleId) {
        Role role = requireReadableRole(roleId);
        List<RolVistaPermiso> permisos = readableViewPermissions(role);

        Map<Integer, RolVentanaConfiguracion> configuracionesVentana = rolVentanaConfiguracionRepository
                .findByRolIdWithVentana(roleId).stream()
                .collect(Collectors.toMap(config -> config.getVentana().getId(), config -> config));
        Map<Integer, RolVistaConfiguracion> configuracionesVista = rolVistaConfiguracionRepository
                .findByRolIdWithVistaAndVentana(roleId).stream()
                .collect(Collectors.toMap(config -> config.getVista().getId(), config -> config));

        Map<Integer, Ventana> ventanas = new LinkedHashMap<>();
        Map<Integer, List<Vista>> vistasPorVentana = new LinkedHashMap<>();
        List<Vista> vistasSueltas = new ArrayList<>();

        for (RolVistaPermiso permiso : permisos) {
            Vista vista = permiso.getVista();
            Ventana ventana = vista.getVentana();
            if (ventana != null && ventana.isActivo()) {
                ventanas.putIfAbsent(ventana.getId(), ventana);
                vistasPorVentana.computeIfAbsent(ventana.getId(), ignored -> new ArrayList<>()).add(vista);
            } else {
                vistasSueltas.add(vista);
            }
        }

        List<RolMenuOrdenItemDTO> resultado = new ArrayList<>();
        ventanas.forEach((ventanaId, ventana) -> {
            RolVentanaConfiguracion config = configuracionesVentana.get(ventanaId);
            RolMenuOrdenItemDTO modulo = toMenuOrderItem(
                    MenuItemType.MODULE, ventana.getId(), ventana.getCodigo(), ventana.getNombre(),
                    ventana.getIcono(), config != null ? config.getOrden() : ventana.getOrden());
            List<RolMenuOrdenItemDTO> vistas = vistasPorVentana.getOrDefault(ventanaId, List.of()).stream()
                    .map(vista -> toViewOrderItem(vista, configuracionesVista.get(vista.getId())))
                    .sorted(menuOrderComparator())
                    .toList();
            modulo.setVistas(new ArrayList<>(vistas));
            resultado.add(modulo);
        });

        vistasSueltas.stream()
                .map(vista -> toViewOrderItem(vista, configuracionesVista.get(vista.getId())))
                .forEach(resultado::add);
        resultado.sort(menuOrderComparator());
        return resultado;
    }

    @Override
    @Transactional
    public List<RolMenuOrdenItemDTO> saveMenuOrder(Integer roleId, List<RolMenuOrdenItemDTO> items) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);
        if (!role.isActivo()) {
            throw new IllegalArgumentException("No se puede ordenar el menú de un rol inactivo");
        }

        List<RolVistaPermiso> permisos = readableViewPermissions(role);
        Map<Integer, Vista> vistasEsperadas = permisos.stream()
                .map(RolVistaPermiso::getVista)
                .collect(Collectors.toMap(Vista::getId, vista -> vista));
        Map<Integer, Ventana> ventanasEsperadas = vistasEsperadas.values().stream()
                .map(Vista::getVentana)
                .filter(Objects::nonNull)
                .filter(Ventana::isActivo)
                .collect(Collectors.toMap(Ventana::getId, ventana -> ventana, (a, b) -> a));

        List<RolMenuOrdenItemDTO> solicitudes = items != null ? items : List.of();
        Set<Integer> modulosRecibidos = new HashSet<>();
        Set<Integer> vistasRecibidas = new HashSet<>();
        Set<Integer> ordenPrincipal = new HashSet<>();

        for (RolMenuOrdenItemDTO item : solicitudes) {
            validateOrder(item, ordenPrincipal);
            if (item.getTipo() == MenuItemType.MODULE) {
                Ventana ventana = ventanasEsperadas.get(item.getReferenciaId());
                if (ventana == null || !modulosRecibidos.add(item.getReferenciaId())) {
                    throw new IllegalArgumentException("El orden contiene módulos duplicados o no asignados al rol");
                }
                Set<Integer> ordenVistas = new HashSet<>();
                for (RolMenuOrdenItemDTO vistaItem : safeChildren(item)) {
                    validateOrder(vistaItem, ordenVistas);
                    Vista vista = vistasEsperadas.get(vistaItem.getReferenciaId());
                    if (vistaItem.getTipo() != MenuItemType.VIEW || vista == null
                            || vista.getVentana() == null
                            || !Objects.equals(vista.getVentana().getId(), ventana.getId())
                            || !vistasRecibidas.add(vista.getId())) {
                        throw new IllegalArgumentException("El módulo contiene vistas duplicadas o inválidas");
                    }
                }
            } else if (item.getTipo() == MenuItemType.VIEW) {
                Vista vista = vistasEsperadas.get(item.getReferenciaId());
                if (vista == null
                        || (vista.getVentana() != null && vista.getVentana().isActivo())
                        || !vistasRecibidas.add(vista.getId())) {
                    throw new IllegalArgumentException("El orden contiene vistas planas duplicadas o inválidas");
                }
            } else {
                throw new IllegalArgumentException("El tipo de elemento del menú es inválido");
            }
        }

        if (!modulosRecibidos.equals(ventanasEsperadas.keySet())
                || !vistasRecibidas.equals(vistasEsperadas.keySet())) {
            throw new IllegalArgumentException("El orden debe incluir exactamente todos los elementos visibles del rol");
        }

        Map<Integer, RolVentanaConfiguracion> configVentanas = rolVentanaConfiguracionRepository
                .findByRolIdWithVentana(roleId).stream()
                .collect(Collectors.toMap(config -> config.getVentana().getId(), config -> config));
        for (RolMenuOrdenItemDTO item : solicitudes) {
            if (item.getTipo() != MenuItemType.MODULE) continue;
            Ventana ventana = ventanasEsperadas.get(item.getReferenciaId());
            RolVentanaConfiguracion config = configVentanas.getOrDefault(ventana.getId(), new RolVentanaConfiguracion());
            if (config.getId() == null) {
                config.setRol(role);
                config.setVentana(ventana);
                config.setPresentacion(ventana.getPresentacionDefault());
            }
            config.setOrden(item.getOrden());
            rolVentanaConfiguracionRepository.save(config);
        }

        rolVistaConfiguracionRepository.deleteByRolId(roleId);
        rolVistaConfiguracionRepository.flush();
        for (RolMenuOrdenItemDTO item : solicitudes) {
            if (item.getTipo() == MenuItemType.VIEW) {
                saveViewOrder(role, vistasEsperadas.get(item.getReferenciaId()), item.getOrden());
            } else {
                for (RolMenuOrdenItemDTO vistaItem : safeChildren(item)) {
                    saveViewOrder(role, vistasEsperadas.get(vistaItem.getReferenciaId()), vistaItem.getOrden());
                }
            }
        }

        incrementarVersionPermisos(role);
        return getMenuOrder(roleId);
    }

    private List<RolVistaPermiso> readableViewPermissions(Role role) {
        return rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(role.getId()).stream()
                .filter(RolVistaPermiso::isLeer)
                .filter(permiso -> permiso.getVista() != null && permiso.getVista().isActivo())
                .filter(permiso -> isAudienceCompatible(role.getScope(), permiso.getVista().getAudience()))
                .toList();
    }

    private RolMenuOrdenItemDTO toViewOrderItem(Vista vista, RolVistaConfiguracion config) {
        return toMenuOrderItem(MenuItemType.VIEW, vista.getId(), vista.getCodigo(), vista.getNombre(),
                vista.getIcono(), config != null ? config.getOrden() : vista.getOrden());
    }

    private RolMenuOrdenItemDTO toMenuOrderItem(
            MenuItemType tipo, Integer referenciaId, String codigo, String nombre, String icono, Integer orden) {
        RolMenuOrdenItemDTO dto = new RolMenuOrdenItemDTO();
        dto.setTipo(tipo);
        dto.setReferenciaId(referenciaId);
        dto.setCodigo(codigo);
        dto.setNombre(nombre);
        dto.setIcono(icono);
        dto.setOrden(orden != null ? orden : 0);
        return dto;
    }

    private Comparator<RolMenuOrdenItemDTO> menuOrderComparator() {
        return Comparator.comparing(RolMenuOrdenItemDTO::getOrden)
                .thenComparing(RolMenuOrdenItemDTO::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RolMenuOrdenItemDTO::getReferenciaId);
    }

    private List<RolMenuOrdenItemDTO> safeChildren(RolMenuOrdenItemDTO item) {
        return item.getVistas() != null ? item.getVistas() : List.of();
    }

    private void validateOrder(RolMenuOrdenItemDTO item, Set<Integer> usedOrders) {
        if (item == null || item.getReferenciaId() == null || item.getOrden() == null || item.getOrden() < 0
                || !usedOrders.add(item.getOrden())) {
            throw new IllegalArgumentException("El orden contiene posiciones duplicadas o inválidas");
        }
    }

    private void saveViewOrder(Role role, Vista vista, Integer orden) {
        RolVistaConfiguracion config = new RolVistaConfiguracion();
        config.setRol(role);
        config.setVista(vista);
        config.setOrden(orden);
        rolVistaConfiguracionRepository.save(config);
    }

    private RolDTO toDTO(Role role) {
        RolDTO dto = new RolDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescripcion(role.getDescripcion());
        dto.setActivo(role.isActivo());
        dto.setCompanyId(role.getCompany() != null ? role.getCompany().getId() : null);
        dto.setScope(role.getScope());
        dto.setPurpose(role.getPurpose());
        dto.setSystemManaged(role.isSystemManaged());
        dto.setProtectedRole(role.isProtectedRole());
        dto.setPermissionVersion(role.getPermissionVersion());
        return dto;
    }

    private Role requireReadableRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (!canReadRole(role)) {
            throw new AccessDeniedException("No tiene acceso a este rol");
        }
        return role;
    }

    private void validarPermisosSolicitados(Role role, List<RolVistaPermisoDTO> permisos) {
        if (permisos == null) {
            throw new IllegalArgumentException("Debe enviar la matriz de permisos");
        }
        Set<Integer> vistasRecibidas = new HashSet<>();
        for (RolVistaPermisoDTO dto : permisos) {
            if (dto.getVistaId() == null || !vistasRecibidas.add(dto.getVistaId())) {
                throw new IllegalArgumentException("La matriz contiene vistas duplicadas o inválidas");
            }
            Vista vista = vistaRepository.findById(dto.getVistaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada: " + dto.getVistaId()));
            if (!vista.isActivo() || !isAudienceCompatible(role.getScope(), vista.getAudience())) {
                throw new AccessDeniedException("La vista no es asignable al alcance del rol: " + vista.getCodigo());
            }
        }
    }

    private boolean isAudienceCompatible(RoleScope scope, ViewAudience audience) {
        if (scope == RoleScope.PLATFORM || audience == ViewAudience.SHARED) {
            return true;
        }
        return (scope == RoleScope.STAFF && audience == ViewAudience.STAFF)
                || (scope == RoleScope.CLIENT && audience == ViewAudience.CLIENT);
    }

    private RolVentanaConfiguracionDTO toMenuConfigurationDTO(
            Ventana ventana,
            RolVentanaConfiguracion configuracion) {
        RolVentanaConfiguracionDTO dto = new RolVentanaConfiguracionDTO();
        dto.setVentanaId(ventana.getId());
        dto.setCodigo(ventana.getCodigo());
        dto.setNombre(ventana.getNombre());
        dto.setIcono(ventana.getIcono());
        dto.setPresentacion(configuracion != null
                ? configuracion.getPresentacion()
                : ventana.getPresentacionDefault());
        dto.setOrden(configuracion != null ? configuracion.getOrden() : ventana.getOrden());
        return dto;
    }

    private void incrementarVersionPermisos(Role role) {
        role.setPermissionVersion(role.getPermissionVersion() + 1);
        roleRepository.save(role);
    }

    private String normalizarNombreRol(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        String normalizado = nombre.trim().toUpperCase().replaceAll("\\s+", "_");
        if (!normalizado.startsWith("ROLE_")) {
            normalizado = "ROLE_" + normalizado;
        }
        if (!normalizado.matches("^ROLE_[A-Z_Ñ]{2,60}$")) {
            throw new IllegalArgumentException("El rol solo puede contener letras y guion bajo");
        }
        return normalizado;
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) return null;
        String value = descripcion.trim();
        if (value.length() > 250) {
            throw new IllegalArgumentException("La descripcion no debe superar 250 caracteres");
        }
        return value;
    }

    private void validarDuplicado(String nombre, Integer companyId) {
        boolean existe = companyId != null
                ? roleRepository.existsByNameAndCompanyId(nombre, companyId)
                : roleRepository.existsByNameAndCompanyIsNull(nombre);
        if (existe) {
            throw new IllegalArgumentException("Ya existe un rol con ese nombre en esta empresa");
        }
    }

    private void validarDuplicadoEdicion(Integer id, String nombre, Integer companyId) {
        if (companyId != null) {
            roleRepository.findByNameAndCompanyId(nombre, companyId)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ya existe un rol con ese nombre en esta empresa");
                    });
        } else {
            roleRepository.findAllByName(nombre).stream()
                    .filter(existing -> !existing.getId().equals(id) && existing.getCompany() == null)
                    .findFirst()
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ya existe un rol con ese nombre en el sistema");
                    });
        }
    }

    private boolean canReadRole(Role role) {
        if (SecurityUtils.isSuperAdmin()) return true;
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        return roleCompanyId != null && Objects.equals(roleCompanyId, SecurityUtils.getCurrentCompanyId());
    }

    private void validateScopeChange(Role role, RoleScope newScope) {
        if (role.getScope() == newScope) return;
        boolean incompatibleGrant = rolVistaPermisoRepository.findByRolId(role.getId()).stream()
                .map(RolVistaPermiso::getVista)
                .anyMatch(vista -> !isAudienceCompatible(newScope, vista.getAudience()));
        if (incompatibleGrant) {
            throw new IllegalArgumentException(
                    "Retire primero los permisos incompatibles antes de cambiar el alcance del rol");
        }
    }

    private void assertCanManageRole(Role role) {
        if (SecurityUtils.isSuperAdmin()) return;
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        if (roleCompanyId == null || !Objects.equals(roleCompanyId, requireCurrentCompany())) {
            throw new AccessDeniedException("No puede modificar un rol global o de otra empresa");
        }
    }

    private void assertCompanyAccess(Integer companyId) {
        if (!SecurityUtils.isSuperAdmin() && !Objects.equals(companyId, requireCurrentCompany())) {
            throw new AccessDeniedException("No tiene acceso a los roles de otra empresa");
        }
    }

    private Integer requireCurrentCompany() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("El usuario no tiene una empresa asignada");
        }
        return companyId;
    }
}
