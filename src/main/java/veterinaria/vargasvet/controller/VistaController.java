package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.VistaRequestDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.service.VistaService;

import java.util.List;
import veterinaria.vargasvet.dto.request.VistaReorderDTO;

@RestController
@RequestMapping("/admin/views")
@RequiredArgsConstructor
public class VistaController {

    private final VistaService vistaService;

    @GetMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<VistaDTO>>> listar(
            @RequestParam(required = false) String grupo) {
        List<VistaDTO> result = grupo != null
                ? vistaService.listarPorGrupo(grupo)
                : vistaService.listarTodas();
        return ResponseEntity.ok(new ApiResponse<>(true, "Vistas obtenidas", result));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<VistaDTO>> crear(@Valid @RequestBody VistaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vista creada", vistaService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<VistaDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody VistaRequestDTO request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vista actualizada", vistaService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        vistaService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vista desactivada", null));
    }

    @PutMapping("/reorder")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> reordenar(@RequestBody List<VistaReorderDTO> reorders) {
        vistaService.reordenar(reorders);
        return ResponseEntity.ok(new ApiResponse<>(true, "Orden actualizado", null));
    }
}
