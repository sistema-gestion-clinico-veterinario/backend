package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Raza;

import java.util.List;

@Repository
public interface RazaRepository extends JpaRepository<Raza, Long> {
    List<Raza> findByEspecieAndActivoTrueOrderByNombreAsc(String especie);
    List<Raza> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndEspecie(String nombre, String especie);
}
