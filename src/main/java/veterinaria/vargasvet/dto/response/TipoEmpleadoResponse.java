package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.entity.TipoEmpleado;

import java.time.LocalDateTime;

public record TipoEmpleadoResponse(Long id, String nombre, String descripcion, Integer companyId, Boolean estado,
                                   Boolean permiteEspecialidades, Boolean esVeterinario,
                                   boolean ejerceComoVeterinario,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static TipoEmpleadoResponse de(TipoEmpleado t) {
        return new TipoEmpleadoResponse(t.getId(), t.getNombre(), t.getDescripcion(),
                t.getCompany() != null ? t.getCompany().getId() : null, t.getEstado(),
                t.getPermiteEspecialidades(), t.getEsVeterinario(), t.ejerceComoVeterinario(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
