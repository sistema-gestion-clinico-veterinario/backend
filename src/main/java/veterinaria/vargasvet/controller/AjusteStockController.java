package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.AjusteStockRequest;
import veterinaria.vargasvet.dto.response.AjusteStockResponse;
import veterinaria.vargasvet.service.AjusteStockService;

@RestController
@RequestMapping("/ajustes-stock")
@RequiredArgsConstructor
public class AjusteStockController {

    private final AjusteStockService ajusteStockService;

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<AjusteStockResponse>>> listarPorProducto(
            @PathVariable Long productoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ajustes de stock obtenidos", ajusteStockService.listarPorProducto(productoId, page, size)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<AjusteStockResponse>> ajustar(@Valid @RequestBody AjusteStockRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock ajustado correctamente", ajusteStockService.ajustar(request)));
    }
}
