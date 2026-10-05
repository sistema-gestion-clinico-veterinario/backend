package veterinaria.vargasvet.dto.response;

import java.time.LocalDateTime;

public record PuntoCobroResponse(
        Long id,
        String nombre,
        boolean activa,
        boolean vinculada,
        String dispositivoInfo,
        LocalDateTime vinculadaAt,
        LocalDateTime ultimoUsoAt,
        boolean esEsteEquipo,
        boolean sesionAbierta,
        String abiertaPorNombre) {
}
