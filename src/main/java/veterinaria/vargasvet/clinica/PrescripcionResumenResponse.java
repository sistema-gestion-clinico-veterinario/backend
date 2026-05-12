package veterinaria.vargasvet.clinica;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String veterinarioNombre;
    private LocalDateTime fechaCreacion;
    // Campos adicionales para listado global
    private String pacienteNombre;
    private String numeroHc;
}
