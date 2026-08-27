package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TipoEmpleadoService {
    Page<TipoEmpleado> findAll(Integer companyId, Pageable pageable);
    TipoEmpleado create(TipoEmpleado tipo);
    TipoEmpleado update(Long id, TipoEmpleado tipo);
    void cambiarEstado(Long id, Boolean activo);
    void delete(Long id);
}
