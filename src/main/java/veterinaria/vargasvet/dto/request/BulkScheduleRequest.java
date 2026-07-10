package veterinaria.vargasvet.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BulkScheduleRequest {
    @NotNull(message = "La fecha de inicio es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @NotEmpty(message = "Debe registrar al menos un turno")
    private List<@Valid HorarioEmpleadoRequest> shifts;
    
    private Boolean overwrite = false;
    
    @JsonFormat(pattern = "HH:mm")
    private java.time.LocalTime originalStartTime;

    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio")
    public boolean isRangoFechasValido() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "El rango de horarios no debe superar 366 dias")
    public boolean isRangoFechasPermitido() {
        if (startDate == null || endDate == null) return true;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) <= 366;
    }
}
