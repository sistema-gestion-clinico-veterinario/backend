package veterinaria.vargasvet.modules.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.modules.users.domain.entity.Menu;
import veterinaria.vargasvet.modules.users.dto.response.MenuDTO;
import veterinaria.vargasvet.modules.users.repository.MenuRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    public List<MenuDTO> getMenuForUser(Set<String> authorities) {
        List<Menu> roots = menuRepository.findByActiveTrueAndParentIsNullOrderBySortOrderAsc();
        return roots.stream()
                .filter(m -> hasAccess(m, authorities))
                .map(m -> convertToDTO(m, authorities))
                .collect(Collectors.toList());
    }

    private boolean hasAccess(Menu menu, Set<String> authorities) {
        if (menu.getRequiredPermission() == null || menu.getRequiredPermission().isEmpty()) {
            return true;
        }
        return authorities.contains("ROLE_SUPER_ADMIN") || authorities.contains(menu.getRequiredPermission());
    }

    private MenuDTO convertToDTO(Menu menu, Set<String> authorities) {
        List<MenuDTO> children = null;
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            children = menu.getChildren().stream()
                    .filter(Menu::isActive)
                    .filter(child -> hasAccess(child, authorities))
                    .map(child -> convertToDTO(child, authorities))
                    .collect(Collectors.toList());
        }

        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setLabel(menu.getLabel());
        dto.setIcon(menu.getIcon());
        dto.setPath(menu.getPath());
        dto.setChildren(children != null && !children.isEmpty() ? children : null);
        return dto;
    }
}
