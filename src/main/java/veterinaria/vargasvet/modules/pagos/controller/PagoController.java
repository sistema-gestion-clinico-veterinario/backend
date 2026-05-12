package veterinaria.vargasvet.modules.pagos.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.modules.pagos.service.PagoService;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<PagoResponse>> registrar(@Valid @RequestBody PagoRequest request) {
        PagoResponse response = pagoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Pago registrado exitosamente", response));
    }

    @GetMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<PagoResponse>> obtenerPorCita(@PathVariable Long citaId) {
        PagoResponse response = pagoService.obtenerPorCita(citaId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pago recuperado con éxito", response));
    }
}
