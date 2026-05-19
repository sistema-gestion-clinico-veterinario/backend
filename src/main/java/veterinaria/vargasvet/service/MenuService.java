package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.domain.entity.Menu;
import veterinaria.vargasvet.dto.response.MenuDTO;
import veterinaria.vargasvet.repository.MenuRepository;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.repository.RoleRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MenuDTO> getMenuForUser(Set<String> authorities) {
        return getMenuForUserAndCompanyAndRole(authorities, null, null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MenuDTO> getMenuForUserAndCompany(Set<String> authorities, Integer companyId) {
        return getMenuForUserAndCompanyAndRole(authorities, companyId, null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MenuDTO> getMenuForUserAndCompanyAndRole(Set<String> authorities, Integer companyId, Integer roleId) {
        List<Menu> roots = menuRepository.findRootMenus();

        if (authorities.contains("ROLE_SUPER_ADMIN") && roleId != null) {
            Role simulatedRole = roleRepository.findById(roleId).orElse(null);
            if (simulatedRole != null) {
                Set<Integer> allowedMenuIds = simulatedRole.getMenus().stream()
                        .map(Menu::getId)
                        .collect(Collectors.toSet());

                List<MenuDTO> rootDTOs = roots.stream()
                        .filter(m -> m.isActive())
                        .filter(m -> hasAccessForCompany(m, allowedMenuIds))
                        .map(m -> convertToDTOForCompany(m, allowedMenuIds))
                        .collect(Collectors.toList());

                return rootDTOs.stream()
                        .map(this::resolveHierarchy)
                        .collect(Collectors.toList());
            }
        }

        if (authorities.contains("ROLE_SUPER_ADMIN") && companyId != null) {
            Set<Integer> companyMenuIds = roleRepository.findByCompanyId(companyId).stream()
                    .flatMap(role -> role.getMenus().stream())
                    .map(Menu::getId)
                    .collect(Collectors.toSet());

            Set<Integer> systemMenuIds = roleRepository.findByCompanyIsNull().stream()
                    .flatMap(role -> role.getMenus().stream())
                    .map(Menu::getId)
                    .collect(Collectors.toSet());

            companyMenuIds.addAll(systemMenuIds);

            List<MenuDTO> rootDTOs = roots.stream()
                    .filter(m -> m.isActive())
                    .filter(m -> hasAccessForCompany(m, companyMenuIds))
                    .map(m -> convertToDTOForCompany(m, companyMenuIds))
                    .collect(Collectors.toList());

            return rootDTOs.stream()
                    .map(this::resolveHierarchy)
                    .collect(Collectors.toList());
        }

        List<MenuDTO> rootDTOs = roots.stream()
                .filter(m -> m.isActive())
                .filter(m -> hasAccess(m, authorities))
                .map(m -> convertToDTO(m, authorities))
                .collect(Collectors.toList());

        return rootDTOs.stream()
                .map(this::resolveHierarchy)
                .collect(Collectors.toList());
    }

    private MenuDTO resolveHierarchy(MenuDTO dto) {
        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            List<MenuDTO> resolvedChildren = dto.getChildren().stream()
                    .map(this::resolveHierarchy)
                    .collect(Collectors.toList());

            if (resolvedChildren.size() == 1 && (dto.getPath() == null || dto.getPath().isBlank())) {
                MenuDTO singleChild = resolvedChildren.get(0);
                return MenuDTO.builder()
                        .id(singleChild.getId())
                        .label(singleChild.getLabel())
                        .icon(singleChild.getIcon() != null && !singleChild.getIcon().isBlank() ? singleChild.getIcon() : dto.getIcon())
                        .path(singleChild.getPath())
                        .children(singleChild.getChildren())
                        .build();
            }

            dto.setChildren(resolvedChildren);
        }
        return dto;
    }

    private boolean hasAccess(Menu menu, Set<String> authorities) {
        if (!menu.isActive()) {
            return false;
        }

        if (authorities.contains("ROLE_SUPER_ADMIN")) {
            return true;
        }
        
        // Si es un menú padre, permitir el acceso si el usuario tiene acceso a al menos uno de sus hijos
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            boolean hasAccessToAnyChild = menu.getChildren().stream()
                    .anyMatch(child -> hasAccess(child, authorities));
            if (hasAccessToAnyChild) {
                return true;
            }
        }
        
        // Si el menú tiene roles asignados, el usuario debe tener al menos uno de ellos
        if (menu.getRoles() != null && !menu.getRoles().isEmpty()) {
            return menu.getRoles().stream()
                    .anyMatch(r -> authorities.contains(r.getName()));
        }
        
        // Fallback de retrocompatibilidad: si no tiene roles asignados pero tiene permiso, verificarlo
        if (menu.getRequiredPermission() != null && !menu.getRequiredPermission().isBlank()) {
            return authorities.contains(menu.getRequiredPermission());
        }
        
        // Si no tiene roles ni permisos asociados, se considera público para todos los logueados
        return true;
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

    private boolean hasAccessForCompany(Menu menu, Set<Integer> allowedMenuIds) {
        if (!menu.isActive()) {
            return false;
        }

        if (allowedMenuIds.contains(menu.getId())) {
            return true;
        }

        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            boolean hasAccessToAnyChild = menu.getChildren().stream()
                    .anyMatch(child -> hasAccessForCompany(child, allowedMenuIds));
            if (hasAccessToAnyChild) {
                return true;
            }
        }

        // Si no tiene roles ni permisos asignados, se considera público para todos
        if ((menu.getRoles() == null || menu.getRoles().isEmpty()) && 
            (menu.getRequiredPermission() == null || menu.getRequiredPermission().isBlank())) {
            return true;
        }

        return false;
    }

    private MenuDTO convertToDTOForCompany(Menu menu, Set<Integer> allowedMenuIds) {
        List<MenuDTO> children = null;
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            children = menu.getChildren().stream()
                    .filter(child -> hasAccessForCompany(child, allowedMenuIds))
                    .map(child -> convertToDTOForCompany(child, allowedMenuIds))
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
