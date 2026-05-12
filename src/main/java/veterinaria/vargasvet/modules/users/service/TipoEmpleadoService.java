package veterinaria.vargasvet.modules.users.service;

import veterinaria.vargasvet.modules.users.domain.entity.TipoEmpleado;
import java.util.List;

public interface TipoEmpleadoService {
    List<TipoEmpleado> findAll(Integer companyId);
    TipoEmpleado create(TipoEmpleado tipo);
    void delete(Long id);
}
