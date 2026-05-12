package veterinaria.vargasvet.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.admin.Menu;
import veterinaria.vargasvet.shared.ApiResponse;
import veterinaria.vargasvet.admin.MenuRepository;
import veterinaria.vargasvet.admin.MenuRequestDTO;


import java.util.List;

@RestController
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class MenuController {

    private final MenuRepository menuRepository;

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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Menú creado exitosamente", menuRepository.save(menu)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Menu>> updateMenu(@PathVariable Integer id, @Valid @RequestBody MenuRequestDTO dto) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menú no encontrado"));
        
        mapDtoToEntity(dto, menu);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú actualizado exitosamente", menuRepository.save(menu)));
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
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Integer id) {
        menuRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Menú eliminado exitosamente", null));
    }
}
