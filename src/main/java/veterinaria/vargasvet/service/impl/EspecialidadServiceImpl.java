package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Especialidad;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.EspecialidadRepository;
import veterinaria.vargasvet.service.EspecialidadService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final veterinaria.vargasvet.repository.CompanyRepository companyRepository;

    @Override
    public List<Especialidad> findAll() {
        Integer companyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (companyId != null) {
            return especialidadRepository.findByCompanyId(companyId);
        }
        return especialidadRepository.findAll();
    }

    @Override
    public Especialidad findById(Long id) {
        return especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public Especialidad create(Especialidad especialidad) {
        Integer companyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (companyId != null) {
            especialidad.setCompany(companyRepository.findById(companyId).orElse(null));
        }
        especialidad.setCreatedAt(LocalDateTime.now());
        return especialidadRepository.save(especialidad);
    }

    @Override
    @Transactional
    public Especialidad update(Long id, Especialidad especialidad) {
        Especialidad existing = findById(id);
        existing.setNombre(especialidad.getNombre());
        existing.setDescripcion(especialidad.getDescripcion());
        existing.setUpdatedAt(LocalDateTime.now());
        return especialidadRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!especialidadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Especialidad no encontrada");
        }
        especialidadRepository.deleteById(id);
    }
}
