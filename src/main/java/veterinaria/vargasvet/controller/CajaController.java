package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.MovimientoCajaResponse;
import veterinaria.vargasvet.dto.response.ResumenCajaResponse;
import veterinaria.vargasvet.dto.request.DetalleCuentaRequest;
import veterinaria.vargasvet.dto.response.CuentaCitaResponse;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;
import veterinaria.vargasvet.service.CuentaCitaService;
import veterinaria.vargasvet.dto.request.AperturaCajaRequest;
import veterinaria.vargasvet.dto.request.ArqueoCajaRequest;
import veterinaria.vargasvet.dto.response.SesionCajaResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;
    private final CuentaCitaService cuentaCitaService;
    private final AuditLogService auditLogService;

    @GetMapping("/sesion")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> obtenerSesion(@RequestParam Integer companyId) {
        SesionCajaResponse sesion = cajaService.obtenerSesionActual(companyId);
        auditLogService.log(companyId, "CONSULTAR_CAJA", "Facturación", "Consultó el estado de la sesión de caja.");
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de caja recuperado", sesion));
    }

    @PostMapping("/sesion/abrir")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> abrirCaja(@Valid @RequestBody AperturaCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Caja abierta", cajaService.abrirCaja(request)));
    }

    @PostMapping("/sesion/arqueo")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> arquearCaja(@Valid @RequestBody ArqueoCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Arqueo registrado", cajaService.arquearCaja(request)));
    }

    @PostMapping("/sesion/cerrar")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> cerrarCaja(@Valid @RequestBody ArqueoCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Caja cerrada", cajaService.cerrarCaja(request)));
    }

    @GetMapping("/cuentas/pendientes")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<Page<CuentaCitaResponse>>> listarPendientes(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CuentaCitaResponse> pendientes = cuentaCitaService.listarPendientes(companyId, page, size);
        auditLogService.log(companyId, "CONSULTAR_CAJA", "Facturación", "Consultó el listado de cuentas pendientes de caja.");
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuentas pendientes recuperadas", pendientes));
    }

    @GetMapping("/cuentas/{citaId}")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> obtenerCuenta(@PathVariable Long citaId) {
        CuentaCitaResponse cuenta = cuentaCitaService.obtener(citaId);
        auditLogService.log("CONSULTAR_CAJA", "Facturación", "Consultó la cuenta de la cita con ID: " + citaId + ".");
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta recuperada", cuenta));
    }

    @PostMapping("/cuentas/{citaId}/detalles")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> agregarDetalle(
            @PathVariable Long citaId, @Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto agregado", cuentaCitaService.agregarDetalle(citaId, request)));
    }

    @PutMapping("/cuentas/{citaId}/detalles/{detalleId}")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> actualizarDetalle(
            @PathVariable Long citaId, @PathVariable Long detalleId,
            @Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto actualizado",
                cuentaCitaService.actualizarDetalle(citaId, detalleId, request)));
    }

    @DeleteMapping("/cuentas/{citaId}/detalles/{detalleId}")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> eliminarDetalle(
            @PathVariable Long citaId, @PathVariable Long detalleId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto eliminado", cuentaCitaService.eliminarDetalle(citaId, detalleId)));
    }

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<Page<MovimientoCajaResponse>>> listar(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MovimientoCajaResponse> movimientos = cajaService.listar(companyId, desde, hasta, page, size);
        auditLogService.log(companyId, "CONSULTAR_CAJA", "Facturación", "Consultó los movimientos de caja.");
        return ResponseEntity.ok(new ApiResponse<>(true, "Movimientos obtenidos", movimientos));
    }

    @GetMapping("/resumen")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<ResumenCajaResponse>> resumen(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ResumenCajaResponse resumen = cajaService.getResumen(companyId, desde, hasta);
        auditLogService.log(companyId, "CONSULTAR_CAJA", "Facturación", "Consultó el resumen de caja.");
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumen obtenido", resumen));
    }

    @PostMapping("/egreso")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarEgreso(
            @Valid @RequestBody MovimientoEgresoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Egreso registrado",
                cajaService.registrarEgreso(request)));
    }

    @PostMapping("/devolucion/{citaId}")
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarDevolucion(
            @PathVariable Long citaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Devolución registrada",
                cajaService.registrarDevolucion(citaId)));
    }
}
