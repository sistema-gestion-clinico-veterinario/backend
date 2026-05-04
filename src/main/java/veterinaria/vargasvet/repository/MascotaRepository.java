package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId")
    List<Mascota> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    @Query(value = "SELECT m FROM Mascota m JOIN m.apoderado a JOIN a.user u " +
                   "WHERE u.company.id = :companyId " +
                   "AND (:nombre IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
                   "AND (:especie IS NULL OR m.especie = :especie) " +
                   "AND (:nombrePropietario IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', :nombrePropietario, '%'))) " +
                   "ORDER BY m.nombreCompleto ASC",
           countQuery = "SELECT COUNT(m) FROM Mascota m JOIN m.apoderado a JOIN a.user u " +
                        "WHERE u.company.id = :companyId " +
                        "AND (:nombre IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
                        "AND (:especie IS NULL OR m.especie = :especie) " +
                        "AND (:nombrePropietario IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', :nombrePropietario, '%')))")
    Page<Mascota> buscar(@Param("companyId") Integer companyId,
                         @Param("nombre") String nombre,
                         @Param("especie") EspecieMascota especie,
                         @Param("nombrePropietario") String nombrePropietario,
                         Pageable pageable);
}
