package veterinaria.vargasvet.modules.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.users.domain.entity.TipoEmpleado;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.users.repository.TipoEmpleadoRepository;
import veterinaria.vargasvet.modules.company.repository.CompanyRepository;
import veterinaria.vargasvet.modules.users.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoEmpleadoServiceImpl implements TipoEmpleadoService {

    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<TipoEmpleado> findAll(Integer companyId) {
        if (companyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(companyId);
        }
        Integer tokenCompanyId = SecurityUtils.getCurrentCompanyId();
        if (tokenCompanyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(tokenCompanyId);
        }
        return tipoEmpleadoRepository.findAll();
    }

    @Override
    @Transactional
    public TipoEmpleado create(TipoEmpleado tipo) {
        Integer companyIdToUse;
        if (SecurityUtils.isSuperAdmin()) {
            if (tipo.getCompany() == null || tipo.getCompany().getId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar una empresa para el tipo de empleado");
            }
            companyIdToUse = tipo.getCompany().getId();
        } else {
            companyIdToUse = SecurityUtils.getCurrentCompanyId();
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
