package veterinaria.vargasvet.modules.company.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CompanyDTO;
import veterinaria.vargasvet.dto.response.CompanyListResponse;
import veterinaria.vargasvet.modules.company.service.CompanyService;

@RestController
@RequestMapping("/admin/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompany() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa obtenidos", companyService.getCompanyInfo()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDTO>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Empresa obtenida con éxito", companyService.findById(id)));
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
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDTO>> saveCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Empresa creada correctamente", companyService.save(companyDTO)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDTO>> updateCompany(@PathVariable Integer id, @Valid @RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa actualizados correctamente", companyService.update(id, companyDTO)));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDTO>> updateCompanyLegacy(@Valid @RequestBody CompanyDTO companyDTO) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa actualizados correctamente", companyService.updateCompanyInfo(companyDTO)));
    }
}
