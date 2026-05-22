package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.Ventana;
import java.util.List;

public interface VentanaRepository extends JpaRepository<Ventana, Integer> {
    Ventana findByCodigo(String codigo);
    List<Ventana> findByActivoTrueOrderByOrdenAsc();
}
