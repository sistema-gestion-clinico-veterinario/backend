package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PrescripcionResumenResponse {
    private Long id;
    private String medicamento;
    private String principioActivo;
    private String dosis;
    private String frecuencia;
    private Integer duracionDias;
    private String viaAdministracion;
    private String instrucciones;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
