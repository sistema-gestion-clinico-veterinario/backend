package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.UnidadMedidaRequest;
import veterinaria.vargasvet.dto.response.UnidadMedidaResponse;
import veterinaria.vargasvet.service.UnidadMedidaService;

import java.util.List;

@RestController
@RequestMapping("/unidades-medida")
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER')")
    public ResponseEntity<ApiResponse<Page<UnidadMedidaResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UnidadMedidaResponse> resultado = unidadMedidaService.listar(companyId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unidades de medida obtenidas", resultado));
    }

    @GetMapping("/activas")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<UnidadMedidaResponse>>> listarActivas(
            @RequestParam(required = false) Integer companyId) {
        List<UnidadMedidaResponse> resultado = unidadMedidaService.listarActivas(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unidades de medida activas", resultado));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> crear(@Valid @RequestBody UnidadMedidaRequest request) {
        UnidadMedidaResponse response = unidadMedidaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Unidad de medida creada exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Unidad de medida actualizada", unidadMedidaService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        unidadMedidaService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unidad de medida desactivada", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", unidadMedidaService.toggleActivo(id)));
    }
}
