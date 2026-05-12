package veterinaria.vargasvet.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.admin.TipoEmpleado;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.admin.TipoEmpleadoRepository;
import veterinaria.vargasvet.admin.TipoEmpleadoService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoEmpleadoServiceImpl implements TipoEmpleadoService {

    private final TipoEmpleadoRepository tipoEmpleadoRepository;
    private final veterinaria.vargasvet.admin.CompanyRepository companyRepository;

    @Override
    public List<TipoEmpleado> findAll(Integer companyId) {
        // Si se pasa companyId explícito (SUPER_ADMIN seleccionó empresa), usar ese
        if (companyId != null) {
            return tipoEmpleadoRepository.findByCompanyId(companyId);
        }
        // Si es ADMIN u otro rol, usar el companyId del token
        Integer tokenCompanyId = veterinaria.vargasvet.shared.SecurityUtils.getCurrentCompanyId();
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
        if (veterinaria.vargasvet.shared.SecurityUtils.isSuperAdmin()) {
            if (tipo.getCompany() == null || tipo.getCompany().getId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar una empresa para el tipo de empleado");
            }
            companyIdToUse = tipo.getCompany().getId();
        } else {
            companyIdToUse = veterinaria.vargasvet.shared.SecurityUtils.getCurrentCompanyId();
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
    public TipoEmpleado update(Long id, TipoEmpleado tipo) {
        TipoEmpleado existing = tipoEmpleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado"));

        existing.setNombre(tipo.getNombre());
        if (tipo.getDescripcion() != null) existing.setDescripcion(tipo.getDescripcion());
        existing.setPermiteEspecialidades(tipo.getPermiteEspecialidades() != null ? tipo.getPermiteEspecialidades() : existing.getPermiteEspecialidades());
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        return tipoEmpleadoRepository.save(existing);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean activo) {
        TipoEmpleado existing = tipoEmpleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de empleado no encontrado"));
        existing.setEstado(activo);
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        tipoEmpleadoRepository.save(existing);
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
