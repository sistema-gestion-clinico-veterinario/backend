package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.service.TipoEmpleadoService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TipoEmpleadoServiceImpl implements TipoEmpleadoService {

    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final veterinaria.vargasvet.repository.CompanyRepository companyRepository;
    private final veterinaria.vargasvet.repository.EmpleadoRepository empleadoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TipoEmpleado> findAll(Integer companyId, Pageable pageable) {
        Integer resolvedCompanyId = resolveCompanyId(companyId);
        return resolvedCompanyId == null
                ? tipoEmpleadoRepository.findAll(pageable)
                : tipoEmpleadoRepository.findByCompanyId(resolvedCompanyId, pageable);
    }

    private void validarNombre(String nombre) {
        if (nombre != null && !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s()\\-]+$")) {
            throw new IllegalArgumentException("El nombre solo puede contener letras, espacios, guiones y paréntesis");
        }
    }

    @Override
    @Transactional
    public TipoEmpleado create(TipoEmpleado tipo) {
        validarNombre(tipo.getNombre());
        Integer companyIdToUse;
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) {
            if (tipo.getCompany() == null || tipo.getCompany().getId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar una empresa para el tipo de empleado");
            }
            companyIdToUse = tipo.getCompany().getId();
        } else {
            companyIdToUse = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
            if (companyIdToUse == null) {
                throw new IllegalArgumentException("No se pudo determinar la empresa del administrador");
            }
        }
        
        tipo.setCompany(companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        tipo.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
        return tipoEmpleadoRepository.save(tipo);
    }

    @Override
    @Transactional
    public TipoEmpleado update(Long id, TipoEmpleado tipo) {
        validarNombre(tipo.getNombre());
        TipoEmpleado existing = tipoEmpleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado"));
        validateCompany(existing);

        existing.setNombre(tipo.getNombre());
        if (tipo.getDescripcion() != null) existing.setDescripcion(tipo.getDescripcion());
        existing.setPermiteEspecialidades(tipo.getPermiteEspecialidades() != null ? tipo.getPermiteEspecialidades() : existing.getPermiteEspecialidades());
        existing.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        return tipoEmpleadoRepository.save(existing);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        TipoEmpleado existing = tipoEmpleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado"));
        validateCompany(existing);

        existing.setEstado(activo);
        existing.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        tipoEmpleadoRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TipoEmpleado existing = tipoEmpleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado"));
        validateCompany(existing);
        if (empleadoRepository.countByTipoEmpleadoId(id) > 0) {
            throw new IllegalArgumentException("No se puede eliminar el tipo de empleado porque tiene empleados asignados");
        }
        tipoEmpleadoRepository.delete(existing);
    }

    private Integer resolveCompanyId(Integer requestedCompanyId) {
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) return requestedCompanyId;
        return veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
    }

    private void validateCompany(TipoEmpleado tipo) {
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) return;
        Integer currentCompanyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (tipo.getCompany() == null || !tipo.getCompany().getId().equals(currentCompanyId)) {
            throw new ResourceNotFoundException("Tipo de empleado no encontrado");
        }
    }
}
