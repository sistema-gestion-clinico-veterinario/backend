package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.TipoVacuna;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.List;

public interface TipoVacunaRepository extends JpaRepository<TipoVacuna, Long> {
    List<TipoVacuna> findByCompanyIdAndEspecieAndActivoTrueOrderByNombre(Integer companyId, EspecieMascota especie);
    boolean existsByCompanyIdAndNombreIgnoreCaseAndEspecie(Integer companyId, String nombre, EspecieMascota especie);
}
