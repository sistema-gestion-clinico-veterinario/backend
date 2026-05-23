package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolVistaPermisoRepository extends JpaRepository<RolVistaPermiso, Integer> {

    List<RolVistaPermiso> findByRolId(Integer rolId);

    @Query("""
            SELECT rvp
            FROM RolVistaPermiso rvp
            JOIN FETCH rvp.vista vista
            LEFT JOIN FETCH vista.ventana
            WHERE rvp.rol.id = :rolId
            """)
    List<RolVistaPermiso> findByRolIdWithVistaAndVentana(@Param("rolId") Integer rolId);

    @Query("SELECT rvp FROM RolVistaPermiso rvp WHERE rvp.rol.id = :rolId AND rvp.vista.codigo = :vistaCodigo")
    Optional<RolVistaPermiso> findByRolIdAndVistaCodigo(@Param("rolId") Integer rolId, @Param("vistaCodigo") String vistaCodigo);

    void deleteByRolId(Integer rolId);
}
