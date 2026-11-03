package veterinaria.vargasvet.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.NotificationReadService;

import java.util.Set;

@Validated
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationReadController {

    private final NotificationReadService service;

    @GetMapping("/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Set<String>>> listRead() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Notificaciones revisadas",
                service.list(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/{notificationKey}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable
            @Pattern(regexp = "^(respaldo-[0-9]+|ia)$", message = "Notificación no reconocida")
            String notificationKey) {
        service.markRead(SecurityUtils.getCurrentUserId(), notificationKey);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notificación marcada como revisada", null));
    }
}
