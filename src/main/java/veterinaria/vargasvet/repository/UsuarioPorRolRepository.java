package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;

import java.util.List;
import java.util.Optional;

public interface UsuarioPorRolRepository extends JpaRepository<UsuarioPorRol, Integer> {

    List<UsuarioPorRol> findByUsuarioId(Integer usuarioId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM UsuarioPorRol upr WHERE upr.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);

    Optional<UsuarioPorRol> findByUsuarioIdAndRolId(Integer usuarioId, Integer rolId);

    @Query("""
            SELECT upr
            FROM UsuarioPorRol upr
            JOIN FETCH upr.rol rol
            LEFT JOIN FETCH rol.company
            WHERE upr.usuario.id = :usuarioId
              AND rol.id = :rolId
              AND rol.activo = true
            """)
    Optional<UsuarioPorRol> findActiveAssignmentByUsuarioIdAndRoleId(
            @Param("usuarioId") Integer usuarioId,
            @Param("rolId") Integer rolId
    );

    boolean existsByUsuarioIdAndRolId(Integer usuarioId, Integer rolId);

    @Query("""
            SELECT COUNT(upr) > 0
            FROM UsuarioPorRol upr
            JOIN upr.usuario u
            JOIN upr.rol r
            WHERE u.email = :email
              AND r.name = :roleName
              AND r.activo = true
              AND (r.company IS NULL OR r.company.id = u.company.id)
            """)
    boolean hasActiveAssignedRole(@Param("email") String email, @Param("roleName") String roleName);

    @Query("SELECT upr FROM UsuarioPorRol upr WHERE upr.usuario.id = :usuarioId")
    List<UsuarioPorRol> findConPermisosByUsuarioId(Integer usuarioId);

    @Query("""
            SELECT DISTINCT upr
            FROM UsuarioPorRol upr
            JOIN FETCH upr.rol rol
            LEFT JOIN FETCH upr.permisos permiso
            LEFT JOIN FETCH permiso.vista vista
            LEFT JOIN FETCH vista.ventana
            WHERE upr.usuario.id = :usuarioId
              AND (:rolActivo IS NULL OR rol.name = :rolActivo)
            """)
    List<UsuarioPorRol> findByUsuarioIdAndRolActivoWithPermisos(
            @Param("usuarioId") Integer usuarioId,
            @Param("rolActivo") String rolActivo
    );

    @Query("""
            SELECT DISTINCT upr
            FROM UsuarioPorRol upr
            JOIN FETCH upr.rol rol
            LEFT JOIN FETCH rol.company
            WHERE upr.usuario.id = :usuarioId
              AND rol.name = :rolActivo
              AND rol.activo = true
            """)
    List<UsuarioPorRol> findActiveAssignmentsByUsuarioIdAndRoleName(
            @Param("usuarioId") Integer usuarioId,
            @Param("rolActivo") String rolActivo
    );
}
