package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.exception.StalePermissionConfigurationException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.RoleService;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    /** Vistas cuya regla de negocio define de forma verificable qué significa "propio". */
    private static final Set<String> DATA_SCOPE_CAPABLE_VIEWS = Set.of(
            "VISTA_CITAS_AGENDA",
            "VISTA_HISTORIAS",
            "VISTA_REPORTES");

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final AuditLogService auditLogService;

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
        if (SecurityUtils.isSuperAdmin()) {
            assertSelectedCompany(companyId);
        } else {
            Integer currentCompanyId = requireCurrentCompany();
            if (companyId != null && !Objects.equals(companyId, currentCompanyId)) {
                throw new AccessDeniedException("No puede crear roles para otra empresa");
            }
            companyId = currentCompanyId;
            if (scope == RoleScope.PLATFORM) {
                throw new AccessDeniedException("Una empresa no puede crear roles de plataforma");
            }
        }
        nombre = normalizarNombreRol(nombre);
        descripcion = normalizarDescripcion(descripcion);
        validateCustomRolePlacement(companyId, scope);
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

        Role saved = roleRepository.save(role);
        auditRole(saved, "CREAR_ROL", "Se creó el rol " + saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RolDTO updateRole(Integer id, String nombre, String descripcion, RoleScope requestedScope) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        if (role.isSystemManaged() || role.isProtectedRole()) {
            throw new IllegalArgumentException("Los roles base del sistema no se pueden modificar");
        }

        nombre = normalizarNombreRol(nombre);
        Integer companyId = role.getCompany() != null ? role.getCompany().getId() : null;
        validarDuplicadoEdicion(id, nombre, companyId);
        role.setName(nombre);
        if (requestedScope != null) {
            RoleScope newScope = requestedScope;
            if (!SecurityUtils.isSuperAdmin() && newScope == RoleScope.PLATFORM) {
                throw new AccessDeniedException("Una empresa no puede convertir un rol en rol de plataforma");
            }
            validateCustomRolePlacement(companyId, newScope);
            validateScopeChange(role, newScope);
            role.setScope(newScope);
        }
        role.setDescripcion(normalizarDescripcion(descripcion));

        Role saved = roleRepository.save(role);
        auditRole(saved, "MODIFICAR_ROL", "Se actualizó el rol " + saved.getName());
        return toDTO(saved);
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
        Role saved = roleRepository.save(role);
        auditRole(saved, saved.isActivo() ? "ACTIVAR_ROL" : "DESACTIVAR_ROL",
                "Se " + (saved.isActivo() ? "activó" : "desactivó") + " el rol " + saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        if (role.isProtectedRole() || role.isSystemManaged()) {
            throw new IllegalArgumentException("No se puede eliminar un rol del sistema");
        }
        if (usuarioPorRolRepository.existsByRolId(id)) {
            throw new IllegalArgumentException(
                    "El rol tiene usuarios asignados y no puede eliminarse. Desactívelo para conservar el historial.");
        }

        Integer companyId = role.getCompany() != null ? role.getCompany().getId() : null;
        String roleName = role.getName();
        rolVentanaPermisoRepository.deleteByRolId(id);
        rolVistaPermisoRepository.deleteByRolId(id);
        roleRepository.delete(role);
        auditLogService.log(companyId, "ELIMINAR_ROL", "Roles",
                "Se eliminó el rol sin asignaciones " + roleName);
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
                    dto.setVisibleMenu(v.isVisibleMenu());
                    if (v.getVentana() != null) {
                        dto.setVentanaId(v.getVentana().getId());
                        dto.setVentanaCodigo(v.getVentana().getCodigo());
                        dto.setVentanaNombre(v.getVentana().getNombre());
                    }
                    dto.setLeer(permiso != null && permiso.isLeer());
                    dto.setEscribir(permiso != null && permiso.isEscribir());
                    dto.setModificar(permiso != null && permiso.isModificar());
                    dto.setEliminar(permiso != null && permiso.isEliminar());
                    dto.setDataScope(permiso != null ? permiso.getDataScope() : veterinaria.vargasvet.domain.enums.DataScope.OWN);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RolVistaPermisoDTO> saveVistasByRole(
            Integer roleId,
            Long expectedVersion,
            List<RolVistaPermisoDTO> permisos) {
        Role role = roleRepository.findByIdForPermissionUpdate(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        assertCanManageRole(role);

        assertRoleConfigurationEditable(role);

        if (expectedVersion == null) {
            throw new IllegalArgumentException("Debe indicar la versión actual de los permisos");
        }
        if (role.getPermissionVersion() != expectedVersion) {
            throw new StalePermissionConfigurationException();
        }

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
            rvp.setDataScope(DATA_SCOPE_CAPABLE_VIEWS.contains(vista.getCodigo())
                    && dto.getDataScope() != null
                    ? dto.getDataScope()
                    : veterinaria.vargasvet.domain.enums.DataScope.OWN);
            rolVistaPermisoRepository.save(rvp);
        }

        incrementarVersionPermisos(role);

        auditLogService.log(
                role.getCompany() != null ? role.getCompany().getId() : null,
                "MODIFICAR_PERMISOS_ROL", "Roles",
                "Se modificaron los permisos del rol " + role.getName() + " (" + permisos.size() + " vistas configuradas)");

        return getVistasByRole(roleId);
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
        dto.setAmbitoEditable(!role.isSystemManaged() && !role.isProtectedRole()
                && !tienePermisosConcedidos(role.getId())
                && !usuarioPorRolRepository.existsByRolId(role.getId()));
        return dto;
    }

    /** Un permiso "concedido" es una fila con al menos un flag en true; una fila toda en
     * false no representa una configuración real todavía. */
    private boolean tienePermisosConcedidos(Integer roleId) {
        return rolVistaPermisoRepository.findByRolId(roleId).stream()
                .anyMatch(p -> p.isLeer() || p.isEscribir() || p.isModificar() || p.isEliminar());
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
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        if (SecurityUtils.isSuperAdmin()) {
            Integer selectedCompanyId = SecurityUtils.getCurrentCompanyId();
            return selectedCompanyId == null || roleCompanyId == null
                    || Objects.equals(roleCompanyId, selectedCompanyId);
        }
        return roleCompanyId != null && Objects.equals(roleCompanyId, SecurityUtils.getCurrentCompanyId());
    }

    /** El ámbito solo se puede cambiar mientras el rol está "vacío": sin ningún permiso
     * concedido y sin ningún usuario asignado todavía. Reforzado también en el backend
     * (no solo ocultando el control en el frontend) porque este endpoint se puede llamar
     * directamente. */
    private void validateScopeChange(Role role, RoleScope newScope) {
        if (role.getScope() == newScope) return;
        if (tienePermisosConcedidos(role.getId())) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el ámbito: el rol ya tiene permisos concedidos. Retírelos primero.");
        }
        if (usuarioPorRolRepository.existsByRolId(role.getId())) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el ámbito: el rol ya tiene usuarios asignados. Reasígnelos primero.");
        }
    }

    private void validateCustomRolePlacement(Integer companyId, RoleScope scope) {
        if (scope == RoleScope.PLATFORM && companyId != null) {
            throw new IllegalArgumentException("Un rol de plataforma no puede pertenecer a una empresa");
        }
        if (scope != RoleScope.PLATFORM && companyId == null) {
            throw new IllegalArgumentException("Los roles de personal o clientes deben pertenecer a una empresa");
        }
    }

    private void assertRoleConfigurationEditable(Role role) {
        if (role.isSystemManaged() || role.isProtectedRole()) {
            throw new IllegalArgumentException("La configuración de un rol base del sistema es inmutable");
        }
    }

    private void auditRole(Role role, String action, String details) {
        Integer companyId = role.getCompany() != null ? role.getCompany().getId() : null;
        auditLogService.log(companyId, action, "Roles", details);
    }

    private void assertCanManageRole(Role role) {
        Integer roleCompanyId = role.getCompany() != null ? role.getCompany().getId() : null;
        if (SecurityUtils.isSuperAdmin()) {
            assertSelectedCompany(roleCompanyId);
            return;
        }
        if (roleCompanyId == null || !Objects.equals(roleCompanyId, requireCurrentCompany())) {
            throw new AccessDeniedException("No puede modificar un rol global o de otra empresa");
        }
    }

    private void assertCompanyAccess(Integer companyId) {
        if (SecurityUtils.isSuperAdmin()) {
            assertSelectedCompany(companyId);
            return;
        }
        if (!Objects.equals(companyId, requireCurrentCompany())) {
            throw new AccessDeniedException("No tiene acceso a los roles de otra empresa");
        }
    }

    private void assertSelectedCompany(Integer requestedCompanyId) {
        Integer selectedCompanyId = SecurityUtils.getCurrentCompanyId();
        if (selectedCompanyId != null && selectedCompanyId > 0 && requestedCompanyId != null
                && !Objects.equals(selectedCompanyId, requestedCompanyId)) {
            throw new AccessDeniedException("La operación no pertenece a la empresa activa");
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
