package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.VentaLibreRequest;
import veterinaria.vargasvet.dto.response.VentaLibreResponse;
import veterinaria.vargasvet.service.VentaLibreService;

@RestController
@RequestMapping("/ventas-libres")
@RequiredArgsConstructor
public class VentaLibreController {

    private final VentaLibreService ventaLibreService;

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CAJA', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<VentaLibreResponse>> registrar(@Valid @RequestBody VentaLibreRequest request) {
        VentaLibreResponse response = ventaLibreService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Venta registrada exitosamente", response));
    }
}
