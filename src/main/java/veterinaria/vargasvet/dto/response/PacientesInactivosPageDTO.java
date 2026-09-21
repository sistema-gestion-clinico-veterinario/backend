package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PacientesInactivosPageDTO {
    private List<ReportesClinicosDTO.PacienteInactivo> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
