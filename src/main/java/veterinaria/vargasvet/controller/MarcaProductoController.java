package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.MarcaProductoRequest;
import veterinaria.vargasvet.dto.response.MarcaProductoResponse;
import veterinaria.vargasvet.service.MarcaProductoService;

import java.util.List;

@RestController
@RequestMapping("/marcas-producto")
@RequiredArgsConstructor
public class MarcaProductoController {
    private final MarcaProductoService marcaProductoService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER')")
    public ResponseEntity<ApiResponse<Page<MarcaProductoResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Marcas obtenidas",
                marcaProductoService.listar(companyId, search, activo, page, size)));
    }

    @GetMapping("/activas")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'LEER') or @accesoValidator.can('VISTA_PRODUCTOS', 'LEER')")
    public ResponseEntity<ApiResponse<List<MarcaProductoResponse>>> listarActivas(
            @RequestParam(required = false) Integer companyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Marcas activas",
                marcaProductoService.listarActivas(companyId)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<MarcaProductoResponse>> crear(@Valid @RequestBody MarcaProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Marca creada exitosamente", marcaProductoService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<MarcaProductoResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody MarcaProductoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Marca actualizada", marcaProductoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<MarcaProductoResponse>> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", marcaProductoService.toggleActivo(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CATEGORIAS_PRODUCTO', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        marcaProductoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Marca eliminada", null));
    }
}
