package veterinaria.vargasvet.pacientes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.pacientes.ApoderadoRequest;
import veterinaria.vargasvet.pacientes.ApoderadoListResponse;
import veterinaria.vargasvet.admin.UserProfileDTO;
import veterinaria.vargasvet.pacientes.ApoderadoService;

@RestController
@RequestMapping("/clientes/apoderados")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;

    @GetMapping
    @PreAuthorize("hasAuthority('APODERADO_READ')")
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
    @PreAuthorize("hasAuthority('APODERADO_READ')")
    public ResponseEntity<ApiResponse<ApoderadoRequest>> findById(@PathVariable Long id) {
        ApoderadoRequest apoderado = apoderadoService.findById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Propietario recuperado con éxito", apoderado));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APODERADO_CREATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerApoderado(@Valid @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.registerApoderado(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Dueño registrado exitosamente", profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('APODERADO_UPDATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateApoderado(@PathVariable Long id, @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.updateApoderado(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos del dueño actualizados exitosamente", profile));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('APODERADO_DELETE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        apoderadoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Propietario eliminado exitosamente", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('APODERADO_STATUS')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @RequestParam Boolean active) {
        apoderadoService.cambiarEstado(id, active);
        String mensaje = active ? "Dueño activado exitosamente" : "Dueño desactivado exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
