package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.service.TipoEmpleadoService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoEmpleadoServiceImpl implements TipoEmpleadoService {

    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final veterinaria.vargasvet.repository.CompanyRepository companyRepository;

    @Override
    public List<TipoEmpleado> findAll() {
        Integer companyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (companyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(companyId);
        }
        return tipoEmpleadoRepository.findAll();
    }

    @Override
    @Transactional
    public TipoEmpleado create(TipoEmpleado tipo) {
        Integer companyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (companyId != null) {
            tipo.setCompany(companyRepository.findById(companyId).orElse(null));
        }
        tipo.setCreatedAt(LocalDateTime.now());
        return tipoEmpleadoRepository.save(tipo);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!tipoEmpleadoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tipo de empleado no encontrado");
        }
        tipoEmpleadoRepository.deleteById(id);
    }
}
