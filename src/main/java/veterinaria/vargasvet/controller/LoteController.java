package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.LoteRequest;
import veterinaria.vargasvet.dto.response.AlertaVencimientoResponse;
import veterinaria.vargasvet.dto.response.LoteResponse;
import veterinaria.vargasvet.service.LoteService;

import java.util.List;

@RestController
@RequestMapping("/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'LEER')")
    public ResponseEntity<ApiResponse<Page<LoteResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lotes obtenidos", loteService.listar(companyId, search, page, size)));
    }

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<LoteResponse>>> listarPorProducto(
            @PathVariable Long productoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lotes del producto obtenidos", loteService.listarPorProducto(productoId, page, size)));
    }

    @GetMapping("/producto/{productoId}/activos")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<LoteResponse>>> listarActivosPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lotes activos del producto obtenidos", loteService.listarActivosPorProducto(productoId)));
    }

    @GetMapping("/alertas")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<AlertaVencimientoResponse>> alertasVencimiento(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Alertas de vencimiento obtenidas", loteService.alertasVencimiento(companyId, dias)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'ESCRIBIR') or @accesoValidator.can('VISTA_PRODUCTOS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<LoteResponse>> crear(@Valid @RequestBody LoteRequest request) {
        LoteResponse response = loteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Lote creado exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'MODIFICAR') or @accesoValidator.can('VISTA_PRODUCTOS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<LoteResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody LoteRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lote actualizado", loteService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'ELIMINAR') or @accesoValidator.can('VISTA_PRODUCTOS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        loteService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lote desactivado", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@accesoValidator.can('VISTA_LOTES', 'MODIFICAR') or @accesoValidator.can('VISTA_PRODUCTOS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<LoteResponse>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", loteService.toggleActivo(id)));
    }
}
