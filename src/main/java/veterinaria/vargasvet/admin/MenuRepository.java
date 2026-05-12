package veterinaria.vargasvet.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.admin.Menu;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Integer> {
    
    @Query("SELECT m FROM Menu m WHERE m.active = true AND m.parent IS NULL ORDER BY m.sortOrder ASC")
    List<Menu> findRootMenus();
}
