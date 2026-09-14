package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.CompanyBrandingResponse;
import veterinaria.vargasvet.service.CompanyService;

/**
 * Endpoints publicos (sin autenticacion) - a diferencia de CompanyController
 * (todo bajo /admin/company, con @PreAuthorize), aqui solo se expone lo
 * minimo necesario para pintar el login antes de que la persona inicie
 * sesion. Ver WebSecurityConfig para el permitAll correspondiente.
 */
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class PublicCompanyController {

    private final CompanyService companyService;

    @GetMapping("/branding/{slug}")
    public ResponseEntity<ApiResponse<CompanyBrandingResponse>> getBrandingBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Marca de la empresa obtenida",
                companyService.findBrandingBySlug(slug)));
    }
}
