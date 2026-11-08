package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClinicalTemplateCreateRequest {
    @NotBlank
    @Size(max = 160)
    private String name;

    @Size(max = 500)
    private String description;

    private List<String> species = new ArrayList<>();
    private List<Long> serviceIds = new ArrayList<>();
    private Long copyFromTemplateId;
}
