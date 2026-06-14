package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.List;

@Repository
public interface RazaRepository extends JpaRepository<Raza, Long> {
    List<Raza> findByEspecieAndActivoTrueOrderByNombreAsc(EspecieMascota especie);
    List<Raza> findByActivoTrueOrderByNombreAsc();
    boolean existsByNombreIgnoreCaseAndEspecie(String nombre, EspecieMascota especie);

    @Query("SELECT r FROM Raza r WHERE r.activo = true " +
           "AND (r.companyId IS NULL OR r.companyId = :companyId) " +
           "AND (:especie IS NULL OR r.especie = :especie) " +
           "ORDER BY r.nombre ASC")
    List<Raza> findByCompanyAndEspecie(@Param("companyId") Long companyId,
                                       @Param("especie") EspecieMascota especie);

    boolean existsByNombreIgnoreCaseAndEspecieAndCompanyId(String nombre, EspecieMascota especie, Long companyId);
}
