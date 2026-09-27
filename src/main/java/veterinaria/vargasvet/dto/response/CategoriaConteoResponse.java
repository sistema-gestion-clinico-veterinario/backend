package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class CategoriaConteoResponse {
    private Long categoriaId;
    private String categoriaNombre;
    private long cantidad;
}
