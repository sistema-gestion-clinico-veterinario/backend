package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final VistaRepository vistaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> getRolesByCompany(Integer companyId) {
        return roleRepository.findByCompanyId(companyId).stream()
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
    public RolDTO createRole(String nombre, String descripcion, Integer companyId) {
        nombre =         normalizarNombreRol(nombre);
        descripcion = normalizarDescripcion(descripcion);
        validarDuplicado(nombre, companyId);

        Role role = new Role();
        role.setName(nombre);
        role.setDescripcion(descripcion);

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
            role.setCompany(company);
        }

        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RolDTO updateRole(Integer id, String nombre, String descripcion) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        boolean esProtegido = role.getName().equals("ROLE_SUPER_ADMIN") || role.getName().equals("ROLE_ADMIN");
        if (!esProtegido) {
            nombre = normalizarNombreRol(nombre);
            Integer companyId = role.getCompany() != null ? role.getCompany().getId() : null;
            validarDuplicadoEdicion(id, nombre, companyId);
            role.setName(nombre);
        }
        role.setDescripcion(normalizarDescripcion(descripcion));

        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RolDTO toggleActivo(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        role.setActivo(!role.isActivo());
        return toDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        if (role.getName().startsWith("ROLE_SUPER_ADMIN") || role.getName().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("No se puede eliminar un rol del sistema");
        }

        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolVistaPermisoDTO> getVistasByRole(Integer roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        List<Vista> todasLasVistas = vistaRepository.findByActivoTrueOrderByNombreAsc();
        Map<Integer, RolVistaPermiso> permisosPorVista = rolVistaPermisoRepository
                .findByRolId(roleId).stream()
                .collect(Collectors.toMap(
                        p -> p.getVista().getId(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return todasLasVistas.stream()
                .map(v -> {
                    RolVistaPermiso permiso = permisosPorVista.get(v.getId());
                    RolVistaPermisoDTO dto = new RolVistaPermisoDTO();
                    dto.setVistaId(v.getId());
                    dto.setCodigo(v.getCodigo());
                    dto.setNombre(v.getNombre());
                    dto.setRuta(v.getRuta());
                    dto.setGrupo(v.getGrupo());
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

        rolVistaPermisoRepository.deleteByRolId(roleId);
        rolVistaPermisoRepository.flush();

        for (RolVistaPermisoDTO dto : permisos) {
            Vista vista = vistaRepository.findById(dto.getVistaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vista no encontrada: " + dto.getVistaId()));
            RolVistaPermiso rvp = new RolVistaPermiso();
            rvp.setRol(role);
            rvp.setVista(vista);
            rvp.setLeer(dto.isLeer());
            rvp.setEscribir(dto.isEscribir());
            rvp.setModificar(dto.isModificar());
            rvp.setEliminar(dto.isEliminar());
            rolVistaPermisoRepository.save(rvp);
        }

        return getVistasByRole(roleId);
    }

    private RolDTO toDTO(Role role) {
        RolDTO dto = new RolDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescripcion(role.getDescripcion());
        dto.setActivo(role.isActivo());
        dto.setCompanyId(role.getCompany() != null ? role.getCompany().getId() : null);
        return dto;
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
            roleRepository.findByName(nombre)
                    .filter(existing -> !existing.getId().equals(id) && existing.getCompany() == null)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ya existe un rol con ese nombre en el sistema");
                    });
        }
    }
}
