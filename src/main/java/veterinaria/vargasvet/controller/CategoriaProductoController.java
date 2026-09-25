package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CategoriaProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaProductoResponse;
import veterinaria.vargasvet.service.CategoriaProductoService;

import java.util.List;

@RestController
@RequestMapping("/categorias-producto")
@RequiredArgsConstructor
public class CategoriaProductoController {

    private final CategoriaProductoService categoriaProductoService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER')")
    public ResponseEntity<ApiResponse<Page<CategoriaProductoResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CategoriaProductoResponse> resultado = categoriaProductoService.listar(companyId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categorías obtenidas", resultado));
    }

    @GetMapping("/activas")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<CategoriaProductoResponse>>> listarActivas(
            @RequestParam(required = false) Integer companyId) {
        List<CategoriaProductoResponse> resultado = categoriaProductoService.listarActivas(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categorías activas", resultado));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> crear(@Valid @RequestBody CategoriaProductoRequest request) {
        CategoriaProductoResponse response = categoriaProductoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Categoría creada exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaProductoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Categoría actualizada", categoriaProductoService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        categoriaProductoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categoría desactivada", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<CategoriaProductoResponse>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", categoriaProductoService.toggleActivo(id)));
    }
}
