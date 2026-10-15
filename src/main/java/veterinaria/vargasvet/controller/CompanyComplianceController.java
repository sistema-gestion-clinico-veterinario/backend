package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ConfigurarCumplimientoClinicaRequest;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;
import veterinaria.vargasvet.dto.response.EstadoCumplimientoClinicaResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CompanyComplianceService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyComplianceController {
    private final CompanyComplianceService service;

    @GetMapping("/platform/companies/{companyId}/compliance")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfiguracionCumplimientoResponse>>> actuales(@PathVariable Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Configuración de cumplimiento", service.actuales(companyId)));
    }

    @GetMapping("/platform/companies/{companyId}/compliance/history")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ConfiguracionCumplimientoResponse>>> historial(@PathVariable Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de cumplimiento", service.historial(companyId)));
    }

    @PostMapping("/platform/companies/{companyId}/compliance/{control}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<ConfiguracionCumplimientoResponse>> configurar(
            @PathVariable Integer companyId, @PathVariable ControlCumplimientoClinica control,
            @Valid @RequestBody ConfigurarCumplimientoClinicaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Nueva versión configurada", service.configurar(companyId, control, request)));
    }

    @GetMapping("/me/compliance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EstadoCumplimientoClinicaResponse>>> miConfiguracion() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Configuración vigente",
                service.actuales(SecurityUtils.getCurrentCompanyId()).stream()
                        .map(EstadoCumplimientoClinicaResponse::desde)
                        .toList()));
    }
}
