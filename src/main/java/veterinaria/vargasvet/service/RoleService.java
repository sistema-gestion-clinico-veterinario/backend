package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;

import java.util.List;

public interface RoleService {
    List<RolDTO> getAllRoles();
    List<RolDTO> getRolesByCompany(Integer companyId);
    List<RolDTO> getSystemRoles();
    RolDTO createRole(String nombre, String descripcion, Integer companyId);
    RolDTO updateRole(Integer id, String nombre, String descripcion);
    void deleteRole(Integer id);

    List<RolVistaPermisoDTO> getVistasByRole(Integer roleId);
    List<RolVistaPermisoDTO> saveVistasByRole(Integer roleId, List<RolVistaPermisoDTO> permisos);
}
