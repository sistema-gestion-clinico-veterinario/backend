package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;

import java.util.List;
import java.util.Optional;

public interface UsuarioPorRolRepository extends JpaRepository<UsuarioPorRol, Integer> {

    List<UsuarioPorRol> findByUsuarioId(Integer usuarioId);

    Optional<UsuarioPorRol> findByUsuarioIdAndRolId(Integer usuarioId, Integer rolId);

    boolean existsByUsuarioIdAndRolId(Integer usuarioId, Integer rolId);

    @Query("SELECT upr FROM UsuarioPorRol upr WHERE upr.usuario.id = :usuarioId")
    List<UsuarioPorRol> findConPermisosByUsuarioId(Integer usuarioId);
}
