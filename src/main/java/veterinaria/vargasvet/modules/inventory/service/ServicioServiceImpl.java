package veterinaria.vargasvet.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.modules.company.domain.entity.Company;
import veterinaria.vargasvet.modules.inventory.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.modules.inventory.dto.ServicioRequest;
import veterinaria.vargasvet.modules.inventory.dto.ServicioResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.modules.company.repository.CompanyRepository;
import veterinaria.vargasvet.modules.inventory.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.modules.users.security.SecurityUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private final ServiciosVeterinariosRepository servicioRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ServicioResponse> listar(Integer companyId, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return servicioRepository.findByCompanyId(
                resolvedCompanyId,
                PageRequest.of(page, size, Sort.by("nombre").ascending())
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioResponse> listarDisponibles(Integer companyId) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        return servicioRepository.findByCompanyIdAndDisponibleTrueAndActivoTrue(resolvedCompanyId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        Integer resolvedCompanyId = resolverCompanyIdParaEscritura(request.getCompanyId());
        Company company = companyRepository.findById(resolvedCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + resolvedCompanyId));

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        servicio.setDisponible(request.getDisponible() != null ? request.getDisponible() : true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(request.getDuracionEstimada());

        return toResponse(servicioRepository.save(servicio));
    }

    @Override
    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        ServiciosVeterinarios servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + id));

        validarPermisoSobreServicio(servicio);

        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setPrecio(request.getPrecio());
        if (request.getDisponible() != null) {
            servicio.setDisponible(request.getDisponible());
        }
        servicio.setDuracionEstimada(request.getDuracionEstimada());

        return toResponse(servicioRepository.save(servicio));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        ServiciosVeterinarios servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + id));
        validarPermisoSobreServicio(servicio);
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    @Override
    @Transactional
    public ServicioResponse toggleDisponible(Long id) {
        ServiciosVeterinarios servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + id));
        validarPermisoSobreServicio(servicio);
        servicio.setDisponible(!Boolean.TRUE.equals(servicio.getDisponible()));
        return toResponse(servicioRepository.save(servicio));
    }

    private ServicioResponse toResponse(ServiciosVeterinarios s) {
        ServicioResponse r = new ServicioResponse();
        r.setId(s.getId());
        r.setNombre(s.getNombre());
        r.setDescripcion(s.getDescripcion());
        r.setPrecio(s.getPrecio());
        r.setDisponible(s.getDisponible());
        r.setActivo(s.getActivo());
        if (s.getCompany() != null) {
            r.setCompanyId(s.getCompany().getId());
            r.setCompanyName(s.getCompany().getName());
        }
        r.setDuracionEstimada(s.getDuracionEstimada());
        return r;
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null)
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    private Integer resolverCompanyIdParaEscritura(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null)
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    private void validarPermisoSobreServicio(ServiciosVeterinarios servicio) {
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (servicio.getCompany() == null || !servicio.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar este servicio");
            }
        }
    }
}
