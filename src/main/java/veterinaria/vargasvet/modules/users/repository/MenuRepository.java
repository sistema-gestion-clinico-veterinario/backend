package veterinaria.vargasvet.modules.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.users.domain.entity.Menu;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {
    List<Menu> findByParentIsNullOrderBySortOrderAsc();
    List<Menu> findByActiveTrueAndParentIsNullOrderBySortOrderAsc();
}
