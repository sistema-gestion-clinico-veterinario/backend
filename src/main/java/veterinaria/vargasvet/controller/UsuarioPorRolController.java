package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.MenuBuilderService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsuarioPorRolController {

    private final MenuBuilderService menuBuilderService;

    @GetMapping("/me/menu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuItemDTO>>> miMenu() {

        Integer usuarioId = SecurityUtils.getCurrentUserId();

        List<MenuItemDTO> menu = menuBuilderService.construirMenu(usuarioId, SecurityUtils.getCurrentRoleId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú del usuario", menu));
    }
}
