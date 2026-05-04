package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.service.ApoderadoService;

@RestController
@RequestMapping("/clientes/apoderados")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Page<ApoderadoListResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ApoderadoListResponse> resultado = apoderadoService.listar(companyId, nombre, numeroDocumento, page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron propietarios" : "Propietarios recuperados con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<ApoderadoRequest>> findById(@PathVariable Long id) {
        ApoderadoRequest apoderado = apoderadoService.findById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Propietario recuperado con éxito", apoderado));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerApoderado(@Valid @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.registerApoderado(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Dueño registrado exitosamente", profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateApoderado(@PathVariable Long id, @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.updateApoderado(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos del dueño actualizados exitosamente", profile));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @RequestParam Boolean active) {
        apoderadoService.cambiarEstado(id, active);
        String mensaje = active ? "Dueño activado exitosamente" : "Dueño desactivado exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
