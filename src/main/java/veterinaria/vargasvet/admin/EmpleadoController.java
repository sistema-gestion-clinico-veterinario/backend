package veterinaria.vargasvet.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.admin.EmpleadoRequest;
import veterinaria.vargasvet.admin.EmpleadoListResponse;
import veterinaria.vargasvet.admin.HorarioEmpleadoResponse;
import veterinaria.vargasvet.admin.UserProfileDTO;
import veterinaria.vargasvet.admin.EmpleadoService;

import java.util.List;

@RestController
@RequestMapping("/admin/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLEADO_READ')")
    public ResponseEntity<ApiResponse<Page<EmpleadoListResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long tipoEmpleadoId,
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EmpleadoListResponse> resultado = empleadoService.listar(companyId, nombre, apellido, email, tipoEmpleadoId, especialidadId, page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron empleados" : "Empleados recuperados con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADO_READ')")
    public ResponseEntity<ApiResponse<EmpleadoRequest>> findById(@PathVariable Long id) {
        EmpleadoRequest empleado = empleadoService.findById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Empleado recuperado con éxito", empleado));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLEADO_CREATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerEmpleado(@Valid @RequestBody EmpleadoRequest dto) {
        UserProfileDTO profile = empleadoService.registerEmpleado(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Empleado registrado exitosamente. Se ha enviado un correo de bienvenida.", profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADO_UPDATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateEmpleado(@PathVariable Long id, @RequestBody EmpleadoRequest dto) {
        UserProfileDTO profile = empleadoService.updateEmpleado(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos del empleado actualizados exitosamente", profile));
    }

    @GetMapping("/{id}/horario")
    @PreAuthorize("hasAuthority('EMPLEADO_READ')")
    public ResponseEntity<ApiResponse<List<HorarioEmpleadoResponse>>> getHorario(@PathVariable Long id) {
        List<HorarioEmpleadoResponse> horario = empleadoService.getHorario(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Horario recuperado con éxito", horario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLEADO_DELETE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        empleadoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Empleado eliminado exitosamente", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLEADO_STATUS')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(@PathVariable Long id, @RequestParam Boolean active) {
        empleadoService.cambiarEstado(id, active);
        String mensaje = active ? "Empleado activado exitosamente" : "Empleado desactivado exitosamente";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, null));
    }
}
