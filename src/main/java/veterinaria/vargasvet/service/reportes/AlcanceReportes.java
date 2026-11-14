package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;

@Component
@RequiredArgsConstructor
public class AlcanceReportes {

    private static final String VISTA_REPORTES = "VISTA_REPORTES";

    private final AccesoValidator accesoValidator;
    private final EmpleadoRepository empleadoRepository;

    public boolean tienePermiso(String vista) {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin() || accesoValidator.can(vista, "LEER");
    }

    public boolean tieneAlcanceEmpresa() {
        return SecurityUtils.isSuperAdmin() || accesoValidator.canAccessCompanyData(VISTA_REPORTES);
    }

    public Integer resolverEmpresa(Integer empresaSolicitada) {
        if (SecurityUtils.isSuperAdmin()) {
            return empresaSolicitada;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    public Long resolverVeterinario(Long veterinarioSolicitado) {
        if (SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin()) {
            return veterinarioSolicitado;
        }
        if (accesoValidator.canAccessCompanyData(VISTA_REPORTES)) {
            return veterinarioSolicitado;
        }
        return empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(
                        SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentCompanyId())
                .map(Empleado::getId)
                .orElse(-1L);
    }
}
