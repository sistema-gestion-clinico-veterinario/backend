package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TipoVacunaResponse {
    private Long id;
    private String nombre;
    private EspecieMascota especie;
    private Integer periodicidadMesesSugerida;
    private BigDecimal precio;
    private String lote;
    private LocalDate fechaVencimientoProducto;
    private BigDecimal dosis;
    private String unidadDosis;
    private String viaAdministracion;
    private Boolean activo;
}
