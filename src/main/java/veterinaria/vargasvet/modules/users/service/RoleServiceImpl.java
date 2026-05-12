package veterinaria.vargasvet.modules.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.users.domain.entity.Permission;
import veterinaria.vargasvet.modules.users.domain.entity.Role;
import veterinaria.vargasvet.modules.users.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.modules.users.dto.response.PermissionDTO;
import veterinaria.vargasvet.modules.users.dto.response.RoleDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.users.repository.PermissionRepository;
import veterinaria.vargasvet.modules.users.repository.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> {
                    PermissionDTO dto = new PermissionDTO();
                    dto.setId(p.getId());
                    dto.setName(p.getName());
                    dto.setLabel(p.getLabel());
                    dto.setModule(p.getModule());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleDTO createRole(RoleCreateDTO dto) {
        if (roleRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("El nombre del rol ya existe");
        }

        Role role = new Role();
        role.setName(dto.getName());
        role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));
        
        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDTO updateRole(Integer id, RoleCreateDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        role.setName(dto.getName());
        role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));
        
        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        
        if (role.getName().startsWith("ROLE_SUPER_ADMIN")) {
            throw new IllegalArgumentException("No se puede eliminar el rol de Super Admin");
        }
        
        roleRepository.delete(role);
    }

    private RoleDTO convertToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setPermissions(role.getPermissions().stream()
                .map(p -> {
                    PermissionDTO pdto = new PermissionDTO();
                    pdto.setId(p.getId());
                    pdto.setName(p.getName());
                    pdto.setLabel(p.getLabel());
                    pdto.setModule(p.getModule());
                    return pdto;
                })
                .collect(Collectors.toSet()));
        return dto;
    }
}
