package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
public class CompanyOperatingHourDTO {
    @NotNull(message = "El dia de atencion es obligatorio")
    private DiaSemana diaSemana;
    private LocalTime openingTime;
    private LocalTime closingTime;

    @NotNull(message = "Debe indicar si la clinica abre ese dia")
    private Boolean isOpen;

    @AssertTrue(message = "Debe indicar hora de apertura y cierre para los dias activos")
    public boolean isHorarioCompletoSiAbre() {
        return !Boolean.TRUE.equals(isOpen) || (openingTime != null && closingTime != null);
    }

    @AssertTrue(message = "La hora de cierre debe ser posterior a la hora de apertura")
    public boolean isRangoHorarioValido() {
        return !Boolean.TRUE.equals(isOpen) || openingTime == null || closingTime == null || closingTime.isAfter(openingTime);
    }
}
