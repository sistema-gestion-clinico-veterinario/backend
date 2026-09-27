package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaConteoResponse;
import veterinaria.vargasvet.dto.response.ProductoResponse;
import veterinaria.vargasvet.service.ProductoService;

import java.util.List;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<ProductoResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductoResponse> resultado = productoService.buscar(companyId, search, categoriaId, activo, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Productos obtenidos", resultado));
    }

    @GetMapping("/conteo-por-categoria")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<CategoriaConteoResponse>>> conteoPorCategoria(
            @RequestParam(required = false) Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Conteo por categoría obtenido", productoService.conteoPorCategoria(companyId)));
    }

    @GetMapping("/activos")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'LEER') or @accesoValidator.can('VISTA_CAJA', 'LEER')")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> listarActivos(
            @RequestParam(required = false) Integer companyId) {
        List<ProductoResponse> resultado = productoService.listarActivos(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Productos activos", resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<ProductoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Producto obtenido", productoService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<ProductoResponse>> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Producto creado exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ProductoResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Producto actualizado", productoService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Producto desactivado", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@accesoValidator.can('VISTA_PRODUCTOS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ProductoResponse>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", productoService.toggleActivo(id)));
    }
}
