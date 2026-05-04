package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.dto.response.CompanyListResponse;
import veterinaria.vargasvet.service.CompanyService;

@RestController
@RequestMapping("/admin/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompany() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa obtenidos", companyService.getCompanyInfo()));
    }

    @GetMapping("/listar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<CompanyListResponse>>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CompanyListResponse> resultado = companyService.listarTodas(page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron empresas" : "Empresas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")

    public ResponseEntity<ApiResponse<CompanyDTO>> saveCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updated = companyService.updateCompanyInfo(companyDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa guardados correctamente", updated));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")

    public ResponseEntity<ApiResponse<CompanyDTO>> updateCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updated = companyService.updateCompanyInfo(companyDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa actualizados correctamente", updated));
    }
}
