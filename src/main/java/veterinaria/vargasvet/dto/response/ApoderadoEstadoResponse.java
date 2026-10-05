package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Qué pasó con las mascotas de una persona cuando se suspendió, dio de baja o reactivó. */
@Data
public class ApoderadoEstadoResponse {
    /** Se pausaron: ya no tienen ninguna persona activa que autorice su atención. */
    private List<String> mascotasPausadas = new ArrayList<>();
    /** Se restauraron: la baja venía de su propietario y ya tienen quien autorice su atención. */
    private List<String> mascotasRestauradas = new ArrayList<>();
    /** Siguen activas porque otra persona activa puede autorizar su atención. */
    private List<String> mascotasQueSiguenActivas = new ArrayList<>();
    /** Al reactivar: siguen inactivas porque el personal las dio de baja por otro motivo; cada una con su motivo. */
    private List<String> mascotasQueSiguenInactivas = new ArrayList<>();
    /** No se pausaron porque tienen citas vigentes; hay que resolverlas primero. */
    private List<String> mascotasConCitasVigentes = new ArrayList<>();
}
