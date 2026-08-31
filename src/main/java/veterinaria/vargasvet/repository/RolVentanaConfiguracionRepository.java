package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.RolVentanaConfiguracion;

import java.util.List;
import java.util.Optional;

public interface RolVentanaConfiguracionRepository extends JpaRepository<RolVentanaConfiguracion, Integer> {

    @Query("""
            SELECT configuracion
            FROM RolVentanaConfiguracion configuracion
            JOIN FETCH configuracion.ventana ventana
            WHERE configuracion.rol.id = :rolId
            """)
    List<RolVentanaConfiguracion> findByRolIdWithVentana(@Param("rolId") Integer rolId);

    Optional<RolVentanaConfiguracion> findByRolIdAndVentanaId(Integer rolId, Integer ventanaId);

    void deleteByRolId(Integer rolId);
}
