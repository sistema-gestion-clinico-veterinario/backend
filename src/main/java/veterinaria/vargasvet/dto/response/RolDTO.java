package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Data
public class RolDTO {
    private Integer id;
    private String name;
    private String descripcion;
    private Boolean activo;
    private Integer companyId;
    private RoleScope scope;
    private RolePurpose purpose;
    private boolean systemManaged;
    private boolean protectedRole;
    private long permissionVersion;
    /** true solo si el rol no tiene ningún permiso concedido ni ningún usuario asignado
     * todavía — cambiar el ámbito (STAFF/CLIENT) después de eso puede dejar huérfanos
     * permisos ya guardados o romper el menú de un usuario activo. */
    private boolean ambitoEditable;
}
