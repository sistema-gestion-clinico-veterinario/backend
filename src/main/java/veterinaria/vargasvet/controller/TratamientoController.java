package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoTratamiento;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.TratamientoRequest;
import veterinaria.vargasvet.dto.response.TratamientoResumenResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.TratamientoService;

import java.util.List;

@RestController
@RequestMapping("/treatments")
@RequiredArgsConstructor
public class TratamientoController {

    private final TratamientoService tratamientoService;
    private final AccesoValidator accesoValidator;

    @PostMapping("/consultation/{consultationId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<TratamientoResumenResponse>> crear(
            @PathVariable("consultationId") Long consultaId,
            @Valid @RequestBody TratamientoRequest request) {
        accesoValidator.validarEscribir("VISTA_HISTORIAS");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tratamiento registrado exitosamente", tratamientoService.crear(consultaId, request)));
    }

    @GetMapping("/consultation/{consultationId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<TratamientoResumenResponse>>> listarPorConsulta(
            @PathVariable("consultationId") Long consultaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Tratamientos obtenidos", tratamientoService.listarPorConsulta(consultaId)));
    }

    @GetMapping("/pets/{petId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<TratamientoResumenResponse>>> listarPorMascota(@PathVariable("petId") Long mascotaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Tratamientos de la mascota obtenidos", tratamientoService.listarPorMascota(mascotaId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<TratamientoResumenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TratamientoRequest request) {
        accesoValidator.validarModificar("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Tratamiento actualizado exitosamente", tratamientoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<TratamientoResumenResponse>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoTratamiento estado) {
        accesoValidator.validarModificar("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado del tratamiento actualizado", tratamientoService.cambiarEstado(id, estado)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        accesoValidator.validarEliminar("VISTA_HISTORIAS");
        tratamientoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tratamiento eliminado exitosamente", null));
    }
}
