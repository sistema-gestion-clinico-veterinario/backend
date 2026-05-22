package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVentanaPermiso;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVentanaPermisoDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;

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
                .filter(r -> r.getName().equals("ROLE_SUPER_ADMIN") || r.getName().equals("ROLE_ADMIN"))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RolDTO createRole(String nombre, String descripcion, Integer companyId) {
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
            role.setName(nombre);
        }
        role.setDescripcion(descripcion);

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
    public List<RolVentanaPermisoDTO> getVentanasByRole(Integer roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        List<Ventana> todasLasVentanas = ventanaRepository.findByActivoTrueOrderByOrdenAsc();
        Map<Integer, RolVentanaPermiso> permisosPorVentana = rolVentanaPermisoRepository
                .findByRolId(roleId).stream()
                .collect(Collectors.toMap(
                        p -> p.getVentana().getId(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return todasLasVentanas.stream()
                .map(v -> {
                    RolVentanaPermiso permiso = permisosPorVentana.get(v.getId());
                    RolVentanaPermisoDTO dto = new RolVentanaPermisoDTO();
                    dto.setVentanaId(v.getId());
                    dto.setCodigo(v.getCodigo());
                    dto.setNombre(v.getNombre());
                    dto.setIcono(v.getIcono());
                    dto.setParentId(v.getParent() != null ? v.getParent().getId() : null);
                    dto.setParentCodigo(v.getParent() != null ? v.getParent().getCodigo() : null);
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
    public List<RolVentanaPermisoDTO> saveVentanasByRole(Integer roleId, List<RolVentanaPermisoDTO> permisos) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        rolVentanaPermisoRepository.deleteByRolId(roleId);
        rolVentanaPermisoRepository.flush();

        for (RolVentanaPermisoDTO dto : permisos) {
            if (!dto.isLeer() && !dto.isEscribir() && !dto.isModificar() && !dto.isEliminar()) continue;
            Ventana ventana = ventanaRepository.findById(dto.getVentanaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ventana no encontrada: " + dto.getVentanaId()));
            RolVentanaPermiso rvp = new RolVentanaPermiso();
            rvp.setRol(role);
            rvp.setVentana(ventana);
            rvp.setLeer(dto.isLeer());
            rvp.setEscribir(dto.isEscribir());
            rvp.setModificar(dto.isModificar());
            rvp.setEliminar(dto.isEliminar());
            rolVentanaPermisoRepository.save(rvp);
        }

        return getVentanasByRole(roleId);
    }

    private RolDTO toDTO(Role role) {
        RolDTO dto = new RolDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescripcion(role.getDescripcion());
        dto.setCompanyId(role.getCompany() != null ? role.getCompany().getId() : null);
        return dto;
    }
}
