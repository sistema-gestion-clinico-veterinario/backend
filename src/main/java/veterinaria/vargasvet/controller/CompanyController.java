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
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'LEER')")
    public ResponseEntity<ApiResponse<CompanyDTO>> getCompany() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa obtenidos", companyService.getCompanyInfo()));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'LEER')")
    public ResponseEntity<ApiResponse<CompanyDTO>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Empresa obtenida con éxito", companyService.findById(id)));
    }

    @GetMapping("/list")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<CompanyListResponse>>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CompanyListResponse> resultado = companyService.listarTodas(page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron empresas" : "Empresas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyDTO>> saveCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO saved = companyService.save(companyDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Empresa creada correctamente", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CompanyDTO>> updateCompany(@PathVariable Integer id, @Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updated = companyService.update(id, companyDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa actualizados correctamente", updated));
    }

    @PutMapping
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CompanyDTO>> updateCompanyLegacy(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO updated = companyService.updateCompanyInfo(companyDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la empresa actualizados correctamente", updated));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<veterinaria.vargasvet.dto.response.CompanyListResponse>> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de empresa actualizado", companyService.toggleActivo(id)));
    }
}
