package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.EstadoMascotaRequest;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;
import veterinaria.vargasvet.service.MascotaService;
import veterinaria.vargasvet.service.AuditLogService;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<Page<MascotaResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EspecieMascota especie,
            @RequestParam(required = false) String nombrePropietario,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MascotaResponse> resultado = mascotaService.listar(companyId, nombre, especie, nombrePropietario, activo, page, size);
        auditLogService.log(companyId, "CONSULTAR_MASCOTAS", "Mascotas", "Consultó la lista de mascotas.");
        String mensaje = resultado.isEmpty() ? "No se encontraron mascotas" : "Mascotas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<MascotaResponse>> obtenerPorId(@PathVariable Long id) {
        MascotaResponse response = mascotaService.obtenerPorId(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascota encontrada", response));
    }

    @GetMapping("/uuid/{uuid}")
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<MascotaResponse>> obtenerPorUuid(@PathVariable String uuid) {
        MascotaResponse response = mascotaService.obtenerPorUuid(uuid);
        return ResponseEntity.ok(new ApiResponse<>(true, "Mascota encontrada", response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PET_CREATE')")
    public ResponseEntity<ApiResponse<MascotaResponse>> registerMascota(@Valid @RequestBody MascotaRequest request) {
        MascotaResponse response = mascotaService.registerMascota(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Mascota registrada exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PET_UPDATE')")
    public ResponseEntity<ApiResponse<MascotaResponse>> updateMascota(@PathVariable Long id, @RequestBody MascotaRequest request) {
        MascotaResponse response = mascotaService.updateMascota(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos de la mascota actualizados exitosamente", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PET_STATUS')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoMascotaRequest request) {
        mascotaService.cambiarEstado(id, request);
        String mensaje = request.getActive() ? "Mascota activada exitosamente" : "Mascota dada de baja exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
