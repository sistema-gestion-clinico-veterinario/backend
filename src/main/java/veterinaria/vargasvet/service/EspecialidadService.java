package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.entity.Especialidad;
import java.util.List;

public interface EspecialidadService {
    List<Especialidad> findAll(Integer companyId);
    Especialidad findById(Long id);
    Especialidad create(Especialidad especialidad);
    Especialidad update(Long id, Especialidad especialidad);
    void delete(Long id);
}
