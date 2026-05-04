package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Apoderado;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {
}
