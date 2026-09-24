package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.company WHERE u.email = :email")
    Optional<Usuario> findByEmailWithCompany(@Param("email") String email);

    /** Preferir sobre findByEmailWithCompany para revalidar la sesion actual - el
     * id es la unica llave que sigue siendo confiable ahora que el correo puede
     * repetirse entre usuarios. */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<Usuario> findByIdWithCompany(@Param("id") Integer id);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.company.id = :companyId")
    Optional<Usuario> findByIdAndCompanyId(@Param("id") Integer id,
                                            @Param("companyId") Integer companyId);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email) AND u.company.id = :companyId")
    Optional<Usuario> findByEmailAndCompanyId(@Param("email") String email,
                                               @Param("companyId") Integer companyId);

    /** DNI, correo y username se validan SOLO dentro de la misma empresa (o, con
     * companyId null, solo entre cuentas sin empresa - SuperAdmin) - dos empresas nunca
     * deben poder detectar ni cruzar datos entre si, aunque sea la misma persona real
     * registrada en ambas con el mismo DNI. Por eso no existe una version global de
     * estos metodos: cada busqueda/validacion exige resolver primero la empresa. */
    boolean existsByEmailIgnoreCaseAndCompanyId(String email, Integer companyId);
    boolean existsByEmailIgnoreCaseAndCompanyIsNull(String email);
    boolean existsByDniAndCompanyId(String dni, Integer companyId);
    boolean existsByDniAndCompanyIsNull(String dni);
    Optional<Usuario> findByDniAndCompanyId(String dni, Integer companyId);
    boolean existsByUsernameIgnoreCaseAndCompanyId(String username, Integer companyId);
    boolean existsByUsernameIgnoreCaseAndCompanyIsNull(String username);

    Optional<Usuario> findByUsernameAndCompanyId(String username, Integer companyId);
    Optional<Usuario> findByUsernameAndCompanyIsNull(String username);
    Optional<Usuario> findByEmailAndCompanyIsNull(String email);

    /** SOLO para el login: con username ya no unico globalmente, puede haber mas de un
     * candidato (uno por empresa) - el login desambigua filtrando por membresia activa
     * en la empresa del slug (ver UsuarioServiceImpl.login). No usar para nada mas. */
    List<Usuario> findAllByUsernameIgnoreCase(String username);
    List<Usuario> findAllByEmailIgnoreCase(String email);

    Optional<Usuario> findByVerificationToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.verificationToken = :token")
    Optional<Usuario> findByVerificationTokenForUpdate(@Param("token") String token);
}
