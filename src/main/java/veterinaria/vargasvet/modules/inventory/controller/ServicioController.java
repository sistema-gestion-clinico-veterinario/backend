package veterinaria.vargasvet.modules.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.modules.inventory.dto.ServicioRequest;
import veterinaria.vargasvet.modules.inventory.dto.ServicioResponse;
import veterinaria.vargasvet.modules.inventory.service.ServicioService;

import java.util.List;

@RestController
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Page<ServicioResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ServicioResponse> resultado = servicioService.listar(companyId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicios obtenidos", resultado));
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<List<ServicioResponse>>> listarDisponibles(
            @RequestParam(required = false) Integer companyId) {
        List<ServicioResponse> resultado = servicioService.listarDisponibles(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicios disponibles", resultado));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> crear(@Valid @RequestBody ServicioRequest request) {
        ServicioResponse response = servicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Servicio creado exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicio actualizado", servicioService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Servicio desactivado", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ServicioResponse>> toggleDisponible(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado actualizado", servicioService.toggleDisponible(id)));
    }
}
