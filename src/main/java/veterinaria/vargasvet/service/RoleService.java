package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.dto.response.PermissionDTO;
import veterinaria.vargasvet.dto.response.RoleDTO;

import java.util.List;

public interface RoleService {
    /** Todos los roles (solo SUPER_ADMIN debería llamar esto) */
    List<RoleDTO> getAllRoles();

    /** Roles pertenecientes a una empresa */
    List<RoleDTO> getRolesByCompany(Integer companyId);

    /** Roles del sistema (company_id IS NULL) */
    List<RoleDTO> getSystemRoles();

    List<PermissionDTO> getAllPermissions();
    RoleDTO createRole(RoleCreateDTO dto);
    RoleDTO updateRole(Integer id, RoleCreateDTO dto);
    void deleteRole(Integer id);
}
