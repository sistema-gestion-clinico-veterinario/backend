package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.PagoService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final AccesoValidator accesoValidator;

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_PAGOS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<PagoResponse>> registrar(@Valid @RequestBody PagoRequest request) {
        PagoResponse response = pagoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Pago registrado exitosamente", response));
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("@accesoValidator.can('VISTA_PAGOS', 'LEER')")
    public ResponseEntity<ApiResponse<PagoResponse>> obtenerPorCita(@PathVariable("appointmentId") Long citaId) {
        PagoResponse response = pagoService.obtenerPorCita(citaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pago recuperado con éxito", response));
    }

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_PAGOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<PagoListResponse>>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer companyId) {
        accesoValidator.validarLeer("VISTA_PAGOS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de pagos recuperado con éxito", pagoService.listarTodos(page, size, companyId)));
    }

    @GetMapping("/portal/my-payments")
    @PreAuthorize("@accesoValidator.can('VISTA_MIS_PAGOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<PagoListResponse>>> misPagos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de pagos recuperado con éxito", pagoService.listarMisPagos(page, size)));
    }

    @GetMapping("/history")
    @PreAuthorize("@accesoValidator.can('VISTA_PAGOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<PagoListResponse>>> listarHistorialPorEmpresa(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer companyId) {
        accesoValidator.validarLeer("VISTA_PAGOS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Historial de pagos recuperado con éxito", pagoService.listarHistorialPorEmpresa(page, size, companyId)));
    }
}
