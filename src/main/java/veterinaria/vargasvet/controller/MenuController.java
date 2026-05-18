package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.Menu;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.repository.MenuRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.dto.request.MenuRequestDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.dto.response.MenuDTO;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.MenuService;
import java.util.Set;
import java.util.stream.Collectors;


import java.util.List;

@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_MANAGE')")
public class MenuController {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final MenuService menuService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Menu>>> getMenuTree() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Árbol de menús obtenido", menuRepository.findRootMenus()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Menu>>> getAllMenusFlat() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista plana de menús obtenida", menuRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Menu>> createMenu(@Valid @RequestBody MenuRequestDTO dto) {
        Menu menu = new Menu();
        mapDtoToEntity(dto, menu);
        Menu savedMenu = menuRepository.save(menu);

        auditLogService.log(
            "CREAR_MENU",
            "Configuración",
            String.format("Se creó el menú '%s' (ID: %d) con ruta '%s'",
                savedMenu.getLabel(), savedMenu.getId(), savedMenu.getPath())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Menú creado exitosamente", savedMenu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Menu>> updateMenu(@PathVariable Integer id, @Valid @RequestBody MenuRequestDTO dto) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menú no encontrado"));
        
        mapDtoToEntity(dto, menu);
        Menu savedMenu = menuRepository.save(menu);

        auditLogService.log(
            "ACTUALIZAR_MENU",
            "Configuración",
            String.format("Se actualizó el menú '%s' (ID: %d)", savedMenu.getLabel(), savedMenu.getId())
        );
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú actualizado exitosamente", savedMenu));
    }

    private void mapDtoToEntity(MenuRequestDTO dto, Menu menu) {
        menu.setLabel(dto.getLabel());
        menu.setIcon(dto.getIcon());
        menu.setPath(dto.getPath());
        menu.setSortOrder(dto.getSortOrder());
        menu.setRequiredPermission(dto.getRequiredPermission());
        menu.setActive(dto.isActive());
        
        if (dto.getParentId() != null) {
            Menu parent = menuRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Padre no encontrado"));
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Integer id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menú no encontrado"));

        // Recopilar recursivamente todos los menús a eliminar (el menú y todos sus hijos)
        List<Menu> menusToDelete = new java.util.ArrayList<>();
        collectMenusToDelete(menu, menusToDelete);

        // Limpiar todas las relaciones en role_menus para estos menús
        for (Role role : roleRepository.findAll()) {
            boolean modified = false;
            for (Menu m : menusToDelete) {
                if (role.getMenus().contains(m)) {
                    role.getMenus().remove(m);
                    modified = true;
                }
            }
            if (modified) {
                roleRepository.save(role);
            }
        }

        // Eliminar físicamente el menú (gracias a CascadeType.ALL, eliminar el padre eliminará a los hijos)
        String menuLabel = menu.getLabel();
        menuRepository.delete(menu);

        auditLogService.log(
            "ELIMINAR_MENU",
            "Configuración",
            String.format("Se eliminó el menú '%s' (ID: %d) y todos sus submenús relacionados", menuLabel, id)
        );
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú y sus relaciones eliminados exitosamente", null));
    }

    @GetMapping("/user-menu")
    public ResponseEntity<ApiResponse<List<MenuDTO>>> getMenuForUser(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) Integer roleId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        java.util.Set<String> authorities = java.util.Collections.emptySet();
        if (auth != null) {
            authorities = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Menú del usuario obtenido",
                menuService.getMenuForUserAndCompanyAndRole(authorities, companyId, roleId)));
    }

    private void collectMenusToDelete(Menu menu, List<Menu> list) {
        list.add(menu);
        if (menu.getChildren() != null) {
            for (Menu child : menu.getChildren()) {
                collectMenusToDelete(child, list);
            }
        }
    }
}
