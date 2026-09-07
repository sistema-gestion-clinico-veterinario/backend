package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentDTO {
    private Long id;
    private LegalDocumentType tipo;
    private String version;
    private String contenido;
    private LocalDateTime vigenteDesde;
}
