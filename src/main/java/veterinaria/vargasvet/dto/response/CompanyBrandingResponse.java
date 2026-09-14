package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta publica (sin autenticacion) para pintar el login con la marca de
 * la empresa resuelta por slug. Deliberadamente minima: nunca expone ruc,
 * email, phone, address, activo, etc. de Company.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBrandingResponse {
    private String name;
    private String logoUrl;
    private String colorPrimario;
}
