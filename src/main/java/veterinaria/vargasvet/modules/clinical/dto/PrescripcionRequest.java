package veterinaria.vargasvet.modules.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PrescripcionRequest {

    @NotBlank(message = "El medicamento es obligatorio")
    private String medicamento;

    private String principioActivo;

    @NotBlank(message = "La dosis es obligatoria")
    private String dosis;

    @NotBlank(message = "La frecuencia es obligatoria")
    private String frecuencia;

    private Integer duracionDias;

    @NotBlank(message = "La vía de administración es obligatoria")
    private String viaAdministracion;

    private String instrucciones;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;
}
