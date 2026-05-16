package veterinaria.vargasvet.dto.request;

import lombok.Data;
import java.time.LocalTime;

@Data
public class CompanyOperatingHourDTO {
    private String diaSemana;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean isOpen;
}
