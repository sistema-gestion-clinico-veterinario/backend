package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.service.VentanaService;

import java.util.List;

@RestController
@RequestMapping("/admin/windows")
@RequiredArgsConstructor
public class VentanaController {

    private final VentanaService ventanaService;

    @GetMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<Ventana>>> listar() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventanas obtenidas", ventanaService.listarTodas()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'LEER')")
    public ResponseEntity<ApiResponse<Ventana>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventana obtenida", ventanaService.obtenerPorId(id)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<Ventana>> crear(@Valid @RequestBody Ventana ventana) {
        Ventana creada = ventanaService.crear(ventana);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Ventana creada exitosamente", creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Ventana>> actualizar(@PathVariable Integer id, @Valid @RequestBody Ventana ventana) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventana actualizada", ventanaService.actualizar(id, ventana)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_VENTANAS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        ventanaService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ventana desactivada", null));
    }
}
