package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.company WHERE u.email = :email")
    Optional<Usuario> findByEmailWithCompany(@Param("email") String email);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    boolean existsByTelefono(String telefono);
    Optional<Usuario> findByVerificationToken(String token);
}
