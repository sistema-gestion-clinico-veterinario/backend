package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import veterinaria.vargasvet.domain.entity.RolVentanaPermiso;

import java.util.List;

public interface RolVentanaPermisoRepository extends JpaRepository<RolVentanaPermiso, Integer> {

    List<RolVentanaPermiso> findByRolId(Integer rolId);

    @Modifying(clearAutomatically = true)
    void deleteByRolId(Integer rolId);

    @Modifying(clearAutomatically = true)
    void deleteByVentanaId(Integer ventanaId);
    void deleteAllByRolId(Integer rolId);
    boolean existsByRolIdAndVentanaId(Integer rolId, Integer ventanaId);
}
