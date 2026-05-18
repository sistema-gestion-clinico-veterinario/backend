package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Menu;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.dto.response.PermissionDTO;
import veterinaria.vargasvet.dto.response.RoleDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MenuRepository;
import veterinaria.vargasvet.repository.PermissionRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getRolesByCompany(Integer companyId) {
        return roleRepository.findByCompanyId(companyId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getSystemRoles() {
        return roleRepository.findByCompanyIsNull().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionDTO(p.getId(), p.getName(), p.getLabel(), p.getDescription(), p.getModule()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleDTO createRole(RoleCreateDTO dto) {
        // Validar nombre único dentro del mismo scope (empresa o sistema)
        if (dto.getCompanyId() != null) {
            if (roleRepository.existsByNameAndCompanyId(dto.getName(), dto.getCompanyId())) {
                throw new IllegalArgumentException("Ya existe un rol con ese nombre en esta empresa");
            }
        } else {
            if (roleRepository.existsByNameAndCompanyIsNull(dto.getName())) {
                throw new IllegalArgumentException("Ya existe un rol de sistema con ese nombre");
            }
        }

        Role role = new Role();
        role.setName(dto.getName());
        role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));

        if (dto.getCompanyId() != null) {
            Company company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
            role.setCompany(company);
        }

        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            role.setMenus(new HashSet<>(menuRepository.findAllById(dto.getMenuIds())));
        }

        Role savedRole = roleRepository.save(role);

        auditLogService.log(
            "CREAR_ROL",
            "Configuración",
            String.format("Se creó el rol '%s' (ID: %d) con %d permisos y %d menús",
                savedRole.getName(), savedRole.getId(),
                savedRole.getPermissions().size(),
                savedRole.getMenus() != null ? savedRole.getMenus().size() : 0)
        );

        return convertToDTO(savedRole);
    }

    @Override
    @Transactional
    public RoleDTO updateRole(Integer id, RoleCreateDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        role.setName(dto.getName());
        role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));

        if (dto.getMenuIds() != null) {
            role.setMenus(new HashSet<>(menuRepository.findAllById(dto.getMenuIds())));
        } else {
            role.getMenus().clear();
        }

        Role savedRole = roleRepository.save(role);

        auditLogService.log(
            "ACTUALIZAR_ROL",
            "Configuración",
            String.format("Se actualizó el rol '%s' (ID: %d) con %d permisos y %d menús",
                savedRole.getName(), savedRole.getId(),
                savedRole.getPermissions().size(),
                savedRole.getMenus() != null ? savedRole.getMenus().size() : 0)
        );

        return convertToDTO(savedRole);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        if (role.getName().startsWith("ROLE_SUPER_ADMIN") || role.getName().equals("ROLE_CLIENTE") || role.getName().equals("ROLE_APODERADO")) {
            throw new IllegalArgumentException("No se puede eliminar este rol del sistema");
        }

        String roleName = role.getName();
        roleRepository.delete(role);

        auditLogService.log(
            "ELIMINAR_ROL",
            "Configuración",
            String.format("Se eliminó el rol '%s' (ID: %d)", roleName, id)
        );
    }

    private RoleDTO convertToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCompanyId(role.getCompany() != null ? role.getCompany().getId() : null);
        dto.setPermissions(role.getPermissions().stream()
                .map(p -> new PermissionDTO(p.getId(), p.getName(), p.getLabel(), p.getDescription(), p.getModule()))
                .collect(Collectors.toSet()));

        if (role.getMenus() != null) {
            dto.setMenuIds(role.getMenus().stream()
                    .map(Menu::getId)
                    .collect(Collectors.toSet()));
        } else {
            dto.setMenuIds(new HashSet<>());
        }

        return dto;
    }
}
