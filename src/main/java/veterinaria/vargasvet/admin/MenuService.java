package veterinaria.vargasvet.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.admin.Menu;
import veterinaria.vargasvet.admin.MenuDTO;
import veterinaria.vargasvet.admin.MenuRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public List<MenuDTO> getMenuForUser(Set<String> authorities) {
        List<Menu> roots = menuRepository.findRootMenus();
        return roots.stream()
                .filter(m -> hasAccess(m, authorities))
                .map(m -> convertToDTO(m, authorities))
                .collect(Collectors.toList());
    }

    private boolean hasAccess(Menu menu, Set<String> authorities) {
        // Si no requiere permiso, es público (o solo requiere estar logueado)
        if (menu.getRequiredPermission() == null || menu.getRequiredPermission().isEmpty()) {
            return true;
        }
        // Verificar si el usuario tiene el permiso requerido o es SUPER_ADMIN
        return authorities.contains("ROLE_SUPER_ADMIN") || authorities.contains(menu.getRequiredPermission());
    }

    private MenuDTO convertToDTO(Menu menu, Set<String> authorities) {
        List<MenuDTO> children = null;
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            children = menu.getChildren().stream()
                    .filter(child -> hasAccess(child, authorities))
                    .map(child -> convertToDTO(child, authorities))
                    .collect(Collectors.toList());
        }

        return MenuDTO.builder()
                .id(menu.getId())
                .label(menu.getLabel())
                .icon(menu.getIcon())
                .path(menu.getPath())
                .children(children != null && !children.isEmpty() ? children : null)
                .build();
    }
}
