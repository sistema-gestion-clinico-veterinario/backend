package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.UsuarioMembresia;

import java.util.List;

@Repository
public interface UsuarioMembresiaRepository extends JpaRepository<UsuarioMembresia, Long> {

    @Query("SELECT um FROM UsuarioMembresia um WHERE um.usuario.id = :usuarioId AND um.estado = true")
    List<UsuarioMembresia> findAllActiveByUsuarioId(@Param("usuarioId") Integer usuarioId);

    boolean existsByUsuarioIdAndCompanyIdAndEstadoTrue(Integer usuarioId, Integer companyId);
}
