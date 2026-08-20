package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.TipoDesparasitante;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.List;

@Repository
public interface TipoDesparasitanteRepository extends JpaRepository<TipoDesparasitante, Long> {

    List<TipoDesparasitante> findByCompanyIdAndEspecieAndActivoTrueOrderByNombre(
            Integer companyId, EspecieMascota especie);

    boolean existsByCompanyIdAndNombreIgnoreCaseAndEspecie(Integer companyId, String nombre, EspecieMascota especie);
}