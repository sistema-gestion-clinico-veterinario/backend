package veterinaria.vargasvet.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.auth.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.admin.UserProfileDTO;
import veterinaria.vargasvet.admin.VeterinarioService;

@RestController
@RequestMapping("/admin/veterinarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class AdminController {

    private final VeterinarioService veterinarioService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileDTO>> registerVeterinario(@Valid @RequestBody VeterinarioRegistrationDTO registrationDTO) {
        UserProfileDTO profile = veterinarioService.registerVeterinario(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Veterinario registrado exitosamente. Se ha enviado un correo con las credenciales.", profile));
    }
}
