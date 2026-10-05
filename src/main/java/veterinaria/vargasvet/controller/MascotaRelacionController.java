package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.MascotaRelacionRequest;
import veterinaria.vargasvet.dto.response.MascotaRelacionResponse;
import veterinaria.vargasvet.service.MascotaRelacionService;

import java.util.List;

@RestController
@RequestMapping("/pets/{mascotaUuid}/relationships")
@RequiredArgsConstructor
public class MascotaRelacionController {

    private final MascotaRelacionService mascotaRelacionService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_MASCOTAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<MascotaRelacionResponse>>> listar(
            @PathVariable String mascotaUuid) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Personas vinculadas recuperadas",
                mascotaRelacionService.listar(mascotaUuid)));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_MASCOTAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<MascotaRelacionResponse>> crear(
            @PathVariable String mascotaUuid,
            @Valid @RequestBody MascotaRelacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Persona vinculada exitosamente",
                        mascotaRelacionService.crear(mascotaUuid, request)));
    }

    @PutMapping("/{relacionUuid}")
    @PreAuthorize("@accesoValidator.can('VISTA_MASCOTAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<MascotaRelacionResponse>> actualizar(
            @PathVariable String mascotaUuid,
            @PathVariable String relacionUuid,
            @Valid @RequestBody MascotaRelacionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Autorizaciones actualizadas",
                mascotaRelacionService.actualizar(mascotaUuid, relacionUuid, request)));
    }

    @DeleteMapping("/{relacionUuid}")
    @PreAuthorize("@accesoValidator.can('VISTA_MASCOTAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> revocar(
            @PathVariable String mascotaUuid,
            @PathVariable String relacionUuid) {
        String aviso = mascotaRelacionService.revocar(mascotaUuid, relacionUuid);
        return ResponseEntity.ok(new ApiResponse<>(true,
                aviso == null ? "Autorización revocada" : "Autorización revocada. " + aviso, null));
    }
}
