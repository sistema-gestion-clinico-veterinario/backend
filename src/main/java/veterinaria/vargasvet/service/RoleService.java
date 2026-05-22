package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVentanaPermisoDTO;

import java.util.List;

public interface RoleService {
    List<RolDTO> getAllRoles();
    List<RolDTO> getRolesByCompany(Integer companyId);
    List<RolDTO> getSystemRoles();
    RolDTO createRole(String nombre, String descripcion, Integer companyId);
    RolDTO updateRole(Integer id, String nombre, String descripcion);
    void deleteRole(Integer id);

    List<RolVentanaPermisoDTO> getVentanasByRole(Integer roleId);
    List<RolVentanaPermisoDTO> saveVentanasByRole(Integer roleId, List<RolVentanaPermisoDTO> permisos);
}
