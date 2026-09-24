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

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);

    /** Para el buscador de clinica del login sin slug - solo empresas activas, nunca
     * expone mas de 10 a la vez (evita que alguien use esto para listar toda la base). */
    List<Company> findTop10ByNameContainingIgnoreCaseAndActivoTrueOrderByNameAsc(String name);
}
