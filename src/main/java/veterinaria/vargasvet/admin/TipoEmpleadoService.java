package veterinaria.vargasvet.admin;

import veterinaria.vargasvet.admin.TipoEmpleado;
import java.util.List;

public interface TipoEmpleadoService {
    List<TipoEmpleado> findAll(Integer companyId);
    TipoEmpleado create(TipoEmpleado tipo);
    TipoEmpleado update(Long id, TipoEmpleado tipo);
    void cambiarEstado(Long id, Boolean activo);
    void delete(Long id);
}
