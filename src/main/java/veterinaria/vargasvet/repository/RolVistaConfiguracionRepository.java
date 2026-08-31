package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.RolVistaConfiguracion;

import java.util.List;

public interface RolVistaConfiguracionRepository extends JpaRepository<RolVistaConfiguracion, Integer> {

    @Query("""
            SELECT configuracion
            FROM RolVistaConfiguracion configuracion
            JOIN FETCH configuracion.vista vista
            LEFT JOIN FETCH vista.ventana
            WHERE configuracion.rol.id = :rolId
            """)
    List<RolVistaConfiguracion> findByRolIdWithVistaAndVentana(@Param("rolId") Integer rolId);

    void deleteByRolId(Integer rolId);
}
