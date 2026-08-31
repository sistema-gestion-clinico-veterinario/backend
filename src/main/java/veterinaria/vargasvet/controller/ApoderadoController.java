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
import veterinaria.vargasvet.service.AuditLogService;

@RestController
@RequestMapping("/clients/guardians")
@RequiredArgsConstructor
public class ApoderadoController {

    private final ApoderadoService apoderadoService;
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'LEER')")
    public ResponseEntity<ApiResponse<Page<ApoderadoListResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ApoderadoListResponse> resultado = apoderadoService.listar(companyId, nombre, numeroDocumento, page, size);
        auditLogService.log(companyId, "CONSULTAR_APODERADOS", "Clientes", "Consultó la lista de propietarios/apoderados.");
        String mensaje = resultado.isEmpty() ? "No se encontraron propietarios" : "Propietarios recuperados con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'LEER')")
    public ResponseEntity<ApiResponse<ApoderadoRequest>> findById(@PathVariable Long id) {
        ApoderadoRequest apoderado = apoderadoService.findById(id);
        auditLogService.log("CONSULTAR_DETALLE_APODERADO", "Clientes", "Consultó el detalle del propietario con ID: " + id + " (" + apoderado.getNombre() + " " + apoderado.getApellido() + ").");
        return ResponseEntity.ok(new ApiResponse<>(true, "Propietario recuperado con éxito", apoderado));
    }

    @PostMapping
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerApoderado(@Valid @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.registerApoderado(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Dueño registrado exitosamente", profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateApoderado(@PathVariable Long id, @Valid @RequestBody ApoderadoRequest dto) {
        UserProfileDTO profile = apoderadoService.updateApoderado(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos del dueño actualizados exitosamente", profile));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        apoderadoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Propietario eliminado exitosamente", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @RequestParam Boolean active) {
        apoderadoService.cambiarEstado(id, active);
        String mensaje = active ? "Dueño activado exitosamente" : "Dueño desactivado exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
