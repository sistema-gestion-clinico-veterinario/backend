package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.service.UsuarioService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileDTO>> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserProfileDTO profile = usuarioService.register(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario registrado exitosamente", profile));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginDTO loginDTO) {
        AuthResponse response = usuarioService.login(loginDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String token) {
        usuarioService.verifyEmail(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Correo verificado exitosamente. Ya puedes iniciar sesión.", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        usuarioService.resendVerificationToken(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Correo de verificación reenviado exitosamente.", null));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(@PathVariable Integer id) {
        UserProfileDTO profile = usuarioService.getProfile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    @PutMapping("/suspend/{id}")
    public ResponseEntity<ApiResponse<Void>> suspendAccount(@PathVariable Integer id) {
        usuarioService.suspendAccount(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta suspendida", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        usuarioService.changePassword(email, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada exitosamente", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = usuarioService.refreshToken(refreshToken);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refrescado exitosamente", response));
    }
}
