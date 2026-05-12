package veterinaria.vargasvet.perfil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.perfil.ProfileUpdateRequest;
import veterinaria.vargasvet.perfil.ProfileResponse;
import veterinaria.vargasvet.perfil.ProfileService;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        ProfileResponse profile = profileService.getMyProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(@RequestBody ProfileUpdateRequest dto) {
        ProfileResponse profile = profileService.updateMyProfile(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil actualizado exitosamente", profile));
    }
}
