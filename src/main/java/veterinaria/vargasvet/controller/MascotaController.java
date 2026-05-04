package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.service.MascotaService;

@RestController
@RequestMapping("/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<MascotaResponse>> registerMascota(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse response = mascotaService.registerMascota(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Mascota registrada exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<MascotaResponse>> updateMascota(@PathVariable Long id, @RequestBody MascotaRequest request) {
        MascotaResponse response = mascotaService.updateMascota(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la mascota actualizados exitosamente", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @Valid @RequestBody veterinaria.vargasvet.dto.request.EstadoMascotaRequest request) {
        mascotaService.cambiarEstado(id, request);
        String mensaje = request.getActive() ? "Mascota activada exitosamente" : "Mascota dada de baja exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
