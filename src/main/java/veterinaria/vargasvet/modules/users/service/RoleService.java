package veterinaria.vargasvet.modules.users.service;

import veterinaria.vargasvet.modules.users.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.modules.users.dto.response.PermissionDTO;
import veterinaria.vargasvet.modules.users.dto.response.RoleDTO;

import java.util.List;

public interface RoleService {
    List<RoleDTO> getAllRoles();
    List<PermissionDTO> getAllPermissions();
    RoleDTO createRole(RoleCreateDTO dto);
    RoleDTO updateRole(Integer id, RoleCreateDTO dto);
    void deleteRole(Integer id);
}
