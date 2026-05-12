package veterinaria.vargasvet.modules.mascotas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.modules.mascotas.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.modules.mascotas.dto.EstadoMascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaResponse;
import veterinaria.vargasvet.modules.mascotas.service.MascotaService;

@RestController
@RequestMapping("/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Page<MascotaResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EspecieMascota especie,
            @RequestParam(required = false) String nombrePropietario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MascotaResponse> resultado = mascotaService.listar(companyId, nombre, especie, nombrePropietario, page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron mascotas" : "Mascotas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

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
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoMascotaRequest request) {
        mascotaService.cambiarEstado(id, request);
        String mensaje = request.getActive() ? "Mascota activada exitosamente" : "Mascota dada de baja exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
