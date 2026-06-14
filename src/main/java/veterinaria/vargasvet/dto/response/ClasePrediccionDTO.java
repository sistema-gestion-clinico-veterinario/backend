package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class ClasePrediccionDTO {
    private Double probability;
    private Boolean positive;
    private Double threshold;
}
