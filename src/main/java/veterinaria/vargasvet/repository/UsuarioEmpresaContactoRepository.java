package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaContacto;

import java.util.Optional;

@Repository
public interface UsuarioEmpresaContactoRepository extends JpaRepository<UsuarioEmpresaContacto, Long> {

    @Query("SELECT c FROM UsuarioEmpresaContacto c WHERE c.usuario.id = :usuarioId AND c.company.id = :companyId")
    Optional<UsuarioEmpresaContacto> findByUsuarioIdAndCompanyId(@Param("usuarioId") Integer usuarioId,
                                                                   @Param("companyId") Integer companyId);

    @Query("SELECT c FROM UsuarioEmpresaContacto c WHERE c.usuario.id = :usuarioId AND c.company IS NULL")
    Optional<UsuarioEmpresaContacto> findByUsuarioIdAndCompanyIsNull(@Param("usuarioId") Integer usuarioId);

    boolean existsByUsuarioIdAndCompanyId(Integer usuarioId, Integer companyId);
}
