package veterinaria.vargasvet.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    
    @NotNull(message = "La lista de turnos es obligatoria")
    private List<HorarioEmpleadoRequest> shifts;
    
    private Boolean overwrite = false;
    
    @JsonFormat(pattern = "HH:mm")
    private java.time.LocalTime originalStartTime;
}
