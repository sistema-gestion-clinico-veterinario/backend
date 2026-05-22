package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import veterinaria.vargasvet.domain.entity.UsuarioPorRolPermiso;

import java.util.List;
import java.util.Optional;

public interface UsuarioPorRolPermisoRepository extends JpaRepository<UsuarioPorRolPermiso, Integer> {

    List<UsuarioPorRolPermiso> findByUsuarioPorRolId(Integer usuarioPorRolId);

    Optional<UsuarioPorRolPermiso> findByUsuarioPorRolIdAndVistaCodigo(Integer usuarioPorRolId, String codigoVista);

    @Query("""
        SELECT p FROM UsuarioPorRolPermiso p
        WHERE p.usuarioPorRol.usuario.id = :usuarioId
        AND p.vista.codigo = :codigoVista
    """)
    List<UsuarioPorRolPermiso> findByUsuarioIdAndVistaCodigo(Integer usuarioId, String codigoVista);

    void deleteByVistaId(Integer vistaId);
}
