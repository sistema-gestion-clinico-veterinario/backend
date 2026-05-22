package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import veterinaria.vargasvet.domain.entity.Ventana;

import java.util.List;
import java.util.Optional;

public interface VentanaRepository extends JpaRepository<Ventana, Integer> {

    Optional<Ventana> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Query("SELECT v FROM Ventana v WHERE v.parent IS NULL AND v.activo = true ORDER BY v.orden ASC")
    List<Ventana> findRaices();

    @Query("SELECT v FROM Ventana v WHERE v.parent IS NULL ORDER BY v.orden ASC")
    List<Ventana> findRaicesAll();

    List<Ventana> findByActivoTrueOrderByOrdenAsc();
}
