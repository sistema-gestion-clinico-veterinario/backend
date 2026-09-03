package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.MenuStructureDTO;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.List;

/**
 * Expone la navegación efectiva de la sesión sin acoplarla al proceso de login.
 * La respuesta sirve para navegación y experiencia de usuario; la autorización
 * real continúa ejecutándose en cada endpoint del backend.
 */
@RestController
@RequestMapping("/me/navigation")
@RequiredArgsConstructor
public class NavigationController {

    private final MenuBuilderService menuBuilderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuStructureDTO>>> getEffectiveNavigation() {
        List<MenuStructureDTO> navigation = menuBuilderService.construirMenuJerarquico(
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentRoleId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Navegación efectiva obtenida", navigation));
    }
}
