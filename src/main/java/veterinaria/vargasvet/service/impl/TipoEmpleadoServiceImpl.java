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
    public List<TipoEmpleado> findAll(Integer companyId) {
        // Si se pasa companyId explícito (SUPER_ADMIN seleccionó empresa), usar ese
        if (companyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(companyId);
        }
        // Si es ADMIN u otro rol, usar el companyId del token
        Integer tokenCompanyId = veterinaria.vargasvet.security.SecurityUtils.getCurrentCompanyId();
        if (tokenCompanyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(tokenCompanyId);
        }
        // SUPER_ADMIN sin companyId: retorna todo
        return tipoEmpleadoRepository.findAll();
    }

    @Override
    @Transactional
    public TipoEmpleado create(TipoEmpleado tipo) {
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
