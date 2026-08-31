package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.dto.response.RolVentanaConfiguracionDTO;
import veterinaria.vargasvet.dto.response.RolMenuOrdenItemDTO;
import veterinaria.vargasvet.domain.enums.RoleScope;

import java.util.List;

public interface RoleService {
    List<RolDTO> getAllRoles();
    List<RolDTO> getRolesByCompany(Integer companyId);
    List<RolDTO> getAssignableRoles(Integer companyId, RoleScope scope);
    List<RolDTO> getSystemRoles();
    RolDTO createRole(String nombre, String descripcion, Integer companyId, RoleScope scope);
    RolDTO updateRole(Integer id, String nombre, String descripcion, RoleScope scope);
    RolDTO toggleActivo(Integer id);
    void deleteRole(Integer id);

    List<RolVistaPermisoDTO> getVistasByRole(Integer roleId);
    List<RolVistaPermisoDTO> saveVistasByRole(Integer roleId, List<RolVistaPermisoDTO> permisos);
    List<RolVentanaConfiguracionDTO> getMenuConfiguration(Integer roleId);
    List<RolVentanaConfiguracionDTO> saveMenuConfiguration(
            Integer roleId,
            List<RolVentanaConfiguracionDTO> configuraciones
    );
    List<RolMenuOrdenItemDTO> getMenuOrder(Integer roleId);
    List<RolMenuOrdenItemDTO> saveMenuOrder(Integer roleId, List<RolMenuOrdenItemDTO> items);
}
