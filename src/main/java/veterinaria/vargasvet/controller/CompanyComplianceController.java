package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ConfigurarCumplimientoClinicaRequest;
import veterinaria.vargasvet.dto.request.ResolverCambioCumplimientoRequest;
import veterinaria.vargasvet.dto.request.SolicitarCambioCumplimientoRequest;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;
import veterinaria.vargasvet.dto.response.EstadoCumplimientoClinicaResponse;
import veterinaria.vargasvet.dto.response.DetalleCumplimientoClinicaResponse;
import veterinaria.vargasvet.dto.response.SolicitudCambioCumplimientoResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.CompanyComplianceService;
import veterinaria.vargasvet.service.SolicitudCambioCumplimientoService;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class CompanyComplianceController {
    private final CompanyComplianceService service;
    private final SolicitudCambioCumplimientoService solicitudes;

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

    @GetMapping("/me/compliance/details")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<List<DetalleCumplimientoClinicaResponse>>> detalleDeMiConfiguracion() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Detalle de cumplimiento",
                service.actuales(SecurityUtils.getCurrentCompanyId()).stream()
                        .map(DetalleCumplimientoClinicaResponse::desde)
                        .toList()));
    }

    @GetMapping("/me/compliance/requests")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<List<SolicitudCambioCumplimientoResponse>>> misSolicitudes() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes de cambio",
                solicitudes.listarDeMiClinica(SecurityUtils.getCurrentCompanyId())));
    }

    @PostMapping("/me/compliance/requests")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<SolicitudCambioCumplimientoResponse>> solicitarCambio(
            @Valid @RequestBody SolicitarCambioCumplimientoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud enviada",
                solicitudes.solicitar(SecurityUtils.getCurrentCompanyId(), request)));
    }

    @GetMapping("/platform/companies/{companyId}/compliance/requests")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<SolicitudCambioCumplimientoResponse>>> solicitudesPendientes(
            @PathVariable Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes pendientes",
                solicitudes.listarPendientes(companyId)));
    }

    @GetMapping("/platform/compliance/requests")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'LEER')")
    public ResponseEntity<ApiResponse<Page<SolicitudCambioCumplimientoResponse>>> bandejaGlobal(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) ControlCumplimientoClinica control,
            @RequestParam(required = false) veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento estado,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bandeja global de solicitudes",
                solicitudes.listarGlobal(companyId, control, estado, desde, hasta, page, size)));
    }

    @GetMapping("/platform/compliance/requests/pending-count")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'LEER')")
    public ResponseEntity<ApiResponse<Long>> solicitudesActivas() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitudes pendientes de decisión",
                solicitudes.contarActivas()));
    }

    @PutMapping("/platform/compliance/requests/{id}/review")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<SolicitudCambioCumplimientoResponse>> iniciarRevision(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud en revisión",
                solicitudes.iniciarRevision(id)));
    }

    @PutMapping("/platform/compliance/requests/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<SolicitudCambioCumplimientoResponse>> resolverSolicitud(
            @PathVariable Long id, @Valid @RequestBody ResolverCambioCumplimientoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                request.aprobar() ? "Solicitud aprobada" : "Solicitud rechazada",
                solicitudes.resolver(id, request)));
    }

    @GetMapping("/me/compliance/request-notifications")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<List<SolicitudCambioCumplimientoResponse>>> notificacionesDeMiClinica() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Decisiones sobre solicitudes",
                solicitudes.notificacionesClinica(SecurityUtils.getCurrentCompanyId())));
    }

    @PutMapping("/me/compliance/requests/{id}/seen")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> marcarDecisionVista(@PathVariable Long id) {
        solicitudes.marcarVistaPorClinica(SecurityUtils.getCurrentCompanyId(), id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notificación revisada", null));
    }
}
