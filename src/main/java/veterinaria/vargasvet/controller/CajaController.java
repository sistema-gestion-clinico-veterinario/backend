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

    @GetMapping("/sesion")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> obtenerSesion(@RequestParam Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de caja recuperado",
                cajaService.obtenerSesionActual(companyId)));
    }

    @PostMapping("/sesion/abrir")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> abrirCaja(@Valid @RequestBody AperturaCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Caja abierta", cajaService.abrirCaja(request)));
    }

    @PostMapping("/sesion/arqueo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> arquearCaja(@Valid @RequestBody ArqueoCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Arqueo registrado", cajaService.arquearCaja(request)));
    }

    @PostMapping("/sesion/cerrar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> cerrarCaja(@Valid @RequestBody ArqueoCajaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Caja cerrada", cajaService.cerrarCaja(request)));
    }

    @GetMapping("/cuentas/pendientes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<Page<CuentaCitaResponse>>> listarPendientes(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuentas pendientes recuperadas",
                cuentaCitaService.listarPendientes(companyId, page, size)));
    }

    @GetMapping("/cuentas/{citaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> obtenerCuenta(@PathVariable Long citaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta recuperada", cuentaCitaService.obtener(citaId)));
    }

    @PostMapping("/cuentas/{citaId}/detalles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> agregarDetalle(
            @PathVariable Long citaId, @Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto agregado", cuentaCitaService.agregarDetalle(citaId, request)));
    }

    @PutMapping("/cuentas/{citaId}/detalles/{detalleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> actualizarDetalle(
            @PathVariable Long citaId, @PathVariable Long detalleId,
            @Valid @RequestBody DetalleCuentaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto actualizado",
                cuentaCitaService.actualizarDetalle(citaId, detalleId, request)));
    }

    @DeleteMapping("/cuentas/{citaId}/detalles/{detalleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<CuentaCitaResponse>> eliminarDetalle(
            @PathVariable Long citaId, @PathVariable Long detalleId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Concepto eliminado", cuentaCitaService.eliminarDetalle(citaId, detalleId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<Page<MovimientoCajaResponse>>> listar(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Movimientos obtenidos",
                cajaService.listar(companyId, desde, hasta, page, size)));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_READ')")
    public ResponseEntity<ApiResponse<ResumenCajaResponse>> resumen(
            @RequestParam Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumen obtenido",
                cajaService.getResumen(companyId, desde, hasta)));
    }

    @PostMapping("/egreso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarEgreso(
            @Valid @RequestBody MovimientoEgresoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Egreso registrado",
                cajaService.registrarEgreso(request)));
    }

    @PostMapping("/devolucion/{citaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasAuthority('SALE_CREATE')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarDevolucion(
            @PathVariable Long citaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Devolución registrada",
                cajaService.registrarDevolucion(citaId)));
    }
}
