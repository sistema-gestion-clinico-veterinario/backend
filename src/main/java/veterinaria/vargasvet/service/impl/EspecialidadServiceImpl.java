package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.service.EspecialidadService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final veterinaria.vargasvet.repository.CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Especialidad> findAll(Integer companyId, Pageable pageable) {
        Integer resolvedCompanyId = resolveCompanyId(companyId);
        return resolvedCompanyId == null
                ? especialidadRepository.findAll(pageable)
                : especialidadRepository.findByCompanyId(resolvedCompanyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Especialidad findById(Long id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + id));
        validateCompany(especialidad);
        return especialidad;
    }

    private void validarNombre(String nombre) {
        if (nombre != null && !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s()\\-]+$")) {
            throw new IllegalArgumentException("El nombre solo puede contener letras, espacios, guiones y paréntesis");
        }
    }

    @Override
    @Transactional
    public Especialidad create(Especialidad especialidad) {
        validarNombre(especialidad.getNombre());
        Integer companyIdToUse;
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) {
            if (especialidad.getCompany() == null || especialidad.getCompany().getId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar una empresa para la especialidad");
            }
            companyIdToUse = especialidad.getCompany().getId();
        } else {
            companyIdToUse = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
            if (companyIdToUse == null) {
                throw new IllegalArgumentException("No se pudo determinar la empresa del administrador");
            }
        }
        
        especialidad.setCompany(companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        especialidad.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
        return especialidadRepository.save(especialidad);
    }

    @Override
    @Transactional
    public Especialidad update(Long id, Especialidad especialidad) {
        validarNombre(especialidad.getNombre());
        Especialidad existing = findById(id);
        existing.setNombre(especialidad.getNombre());
        existing.setDescripcion(especialidad.getDescripcion());
        existing.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        return especialidadRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        especialidadRepository.delete(findById(id));
    }

    private Integer resolveCompanyId(Integer requestedCompanyId) {
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) return requestedCompanyId;
        return veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
    }

    private void validateCompany(Especialidad especialidad) {
        if (veterinaria.vargasvet.security.SecurityUtils.isSuperAdmin()) return;
        Integer currentCompanyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (especialidad.getCompany() == null || !especialidad.getCompany().getId().equals(currentCompanyId)) {
            throw new ResourceNotFoundException("Especialidad no encontrada");
        }
    }
}
