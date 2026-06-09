package veterinaria.vargasvet.dto.request;

import lombok.Data;

@Data
public class VistaReorderDTO {
    private Integer id;
    private Integer orden;
    private String grupo;
    private Integer ordenGrupo;
}
