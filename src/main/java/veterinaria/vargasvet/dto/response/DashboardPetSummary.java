package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

/** Datos mínimos de una mascota mostrados en el dashboard. */
@Data
@Builder
public class DashboardPetSummary {
    private Long id;
    private String nombreCompleto;
    private EspecieMascota especie;
    private String raza;
}
