package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioEmpresaCredencialRepository extends JpaRepository<UsuarioEmpresaCredencial, Long> {

    @Query("SELECT c FROM UsuarioEmpresaCredencial c WHERE c.usuario.id = :usuarioId AND c.company.id = :companyId")
    Optional<UsuarioEmpresaCredencial> findByUsuarioIdAndCompanyId(@Param("usuarioId") Integer usuarioId,
                                                                     @Param("companyId") Integer companyId);

    @Query("SELECT c FROM UsuarioEmpresaCredencial c WHERE c.usuario.id = :usuarioId AND c.company IS NULL")
    Optional<UsuarioEmpresaCredencial> findByUsuarioIdAndCompanyIsNull(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT c FROM UsuarioEmpresaCredencial c WHERE c.usuario.id = :usuarioId")
    List<UsuarioEmpresaCredencial> findAllByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT DISTINCT c.usuario.id FROM UsuarioEmpresaCredencial c " +
           "WHERE c.usuario.id IN :usuarioIds AND c.passwordChanged = true")
    java.util.Set<Integer> usuariosConContrasenaCreada(@Param("usuarioIds") java.util.Collection<Integer> usuarioIds);

    boolean existsByUsuarioIdAndCompanyId(Integer usuarioId, Integer companyId);
}
