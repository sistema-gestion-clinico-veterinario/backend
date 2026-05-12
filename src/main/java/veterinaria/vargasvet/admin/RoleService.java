package veterinaria.vargasvet.admin;

import veterinaria.vargasvet.admin.RoleCreateDTO;
import veterinaria.vargasvet.admin.PermissionDTO;
import veterinaria.vargasvet.admin.RoleDTO;

import java.util.List;

public interface RoleService {
    List<RoleDTO> getAllRoles();
    List<PermissionDTO> getAllPermissions();
    RoleDTO createRole(RoleCreateDTO dto);
    RoleDTO updateRole(Integer id, RoleCreateDTO dto);
    void deleteRole(Integer id);
}
