package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.company WHERE u.email = :email")
    Optional<Usuario> findByEmailWithCompany(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.company.id = :companyId")
    Optional<Usuario> findByIdAndCompanyId(@Param("id") Integer id,
                                            @Param("companyId") Integer companyId);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email) AND u.company.id = :companyId")
    Optional<Usuario> findByEmailAndCompanyId(@Param("email") String email,
                                               @Param("companyId") Integer companyId);

    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    boolean existsByTelefono(String telefono);
    Optional<Usuario> findByVerificationToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.verificationToken = :token")
    Optional<Usuario> findByVerificationTokenForUpdate(@Param("token") String token);
}
