package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.EmpleadoRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.service.EmpleadoService;

@RestController
@RequestMapping("/admin/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'USER_CREATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerEmpleado(@Valid @RequestBody EmpleadoRegistrationDTO dto) {
        UserProfileDTO profile = empleadoService.registerEmpleado(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Empleado registrado exitosamente. Se ha enviado un correo de bienvenida.", profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateEmpleado(@PathVariable Integer id, @RequestBody veterinaria.vargasvet.dto.request.EmpleadoUpdateDTO dto) {
        UserProfileDTO profile = empleadoService.updateEmpleado(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos del empleado actualizados exitosamente", profile));
    }
}
