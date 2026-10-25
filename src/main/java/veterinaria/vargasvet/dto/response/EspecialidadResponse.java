package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.entity.Especialidad;

import java.time.LocalDateTime;

public record EspecialidadResponse(Long id, String nombre, String descripcion, Integer companyId,
                                   LocalDateTime createdAt, LocalDateTime updatedAt, boolean eliminable) {

    public static EspecialidadResponse de(Especialidad e) {
        return de(e, true);
    }

    public static EspecialidadResponse de(Especialidad e, boolean eliminable) {
        return new EspecialidadResponse(e.getId(), e.getNombre(), e.getDescripcion(),
                e.getCompany() != null ? e.getCompany().getId() : null, e.getCreatedAt(), e.getUpdatedAt(), eliminable);
    }
}
