package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ProfileUpdateRequest;
import veterinaria.vargasvet.dto.response.ProfileResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.ProfileService;

import veterinaria.vargasvet.service.AuditLogService;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuditLogService auditLogService;
    private final AccesoValidator accesoValidator;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        ProfileResponse profile = profileService.getMyProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMySchedule() {
        ProfileResponse profile = profileService.getMyProfile();
        auditLogService.log("CONSULTAR_HORARIO", "Horario", "El empleado consultó su propio horario.");
        return ResponseEntity.ok(new ApiResponse<>(true, "Horario obtenido", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(@RequestBody ProfileUpdateRequest dto) {
        accesoValidator.validarModificar("VISTA_PROFILE");
        ProfileResponse profile = profileService.updateMyProfile(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil actualizado exitosamente", profile));
    }
}
