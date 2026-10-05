package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Company;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    /** Resuelve la empresa a partir del slug de la URL (systemvet.com/<slug>/login). */
    Optional<Company> findBySlug(String slug);

    /** Bloquea la fila de la empresa hasta que termine la transacción: las operaciones que pueden dejarla sin
     * administradores se atienden de una en una. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Company c WHERE c.id = :id")
    Optional<Company> lockById(@org.springframework.data.repository.query.Param("id") Integer id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);

    /** Para el buscador de clinica del login sin slug - solo empresas activas, nunca
     * expone mas de 10 a la vez (evita que alguien use esto para listar toda la base). */
    List<Company> findTop10ByNameContainingIgnoreCaseAndActivoTrueOrderByNameAsc(String name);
}
