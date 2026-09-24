package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado publico (sin autenticacion) del buscador de clinica en el login sin slug.
 * Deliberadamente minimo, igual que CompanyBrandingResponse - nunca expone ruc, email,
 * phone, address, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanySearchResultResponse {
    private String name;
    private String slug;
    private String logoUrl;
}
