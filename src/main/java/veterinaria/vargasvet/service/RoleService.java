package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.dto.response.PermissionDTO;
import veterinaria.vargasvet.dto.response.RoleDTO;

import java.util.List;

public interface RoleService {
    List<RoleDTO> getAllRoles();
    List<PermissionDTO> getAllPermissions();
    RoleDTO createRole(RoleCreateDTO dto);
    RoleDTO updateRole(Integer id, RoleCreateDTO dto);
    void deleteRole(Integer id);
}
