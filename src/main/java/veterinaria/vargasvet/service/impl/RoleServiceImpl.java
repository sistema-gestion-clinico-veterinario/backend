package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.dto.response.PermissionDTO;
import veterinaria.vargasvet.dto.response.RoleDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.PermissionRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.service.RoleService;

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
                .map(p -> new PermissionDTO(p.getId(), p.getName(), p.getLabel(), p.getDescription(), p.getModule()))
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
                .map(p -> new PermissionDTO(p.getId(), p.getName(), p.getLabel(), p.getDescription(), p.getModule()))
                .collect(Collectors.toSet()));
        return dto;
    }
}
