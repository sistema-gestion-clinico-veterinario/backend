package veterinaria.vargasvet.dto.response;

import java.util.List;

public record UsuarioRolResponse(
        Integer id,
        Integer userId,
        Integer roleId,
        String roleName,
        List<PermisoResponse> permisos
) {
    public record PermisoResponse(
            Integer id,
            Integer vistaId,
            String vistaCodigo,
            boolean leer,
            boolean escribir,
            boolean modificar,
            boolean eliminar
    ) {
    }
}
