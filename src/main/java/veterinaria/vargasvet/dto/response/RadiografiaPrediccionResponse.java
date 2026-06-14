package veterinaria.vargasvet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RadiografiaPrediccionResponse {

    private String model;

    @JsonProperty("file_type")
    private String fileType;

    private Map<String, ClasePrediccionDTO> predictions;

    private List<String> diagnoses;

    @JsonProperty("inference_ms")
    private Double inferenceMs;
}
