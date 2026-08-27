package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.entity.Especialidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EspecialidadService {
    Page<Especialidad> findAll(Integer companyId, Pageable pageable);
    Especialidad findById(Long id);
    Especialidad create(Especialidad especialidad);
    Especialidad update(Long id, Especialidad especialidad);
    void delete(Long id);
}
