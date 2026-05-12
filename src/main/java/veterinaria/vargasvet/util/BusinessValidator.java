package veterinaria.vargasvet.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;

/**
 * Validaciones de negocio reutilizables entre servicios.
 */
@Component
@RequiredArgsConstructor
public class BusinessValidator {

    private final CompanyRepository companyRepository;

    /**
     * Lanza excepción si la empresa con el ID dado está inactiva.
     * Si companyId es null (super admin sin empresa asignada), no hace nada.
     */
    public void checkCompanyActiva(Integer companyId) {
        if (companyId == null) return;
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company != null && !company.isActivo()) {
            throw new IllegalStateException(
                "La empresa está inactiva. No se pueden realizar operaciones de escritura.");
        }
    }

    /**
     * Lanza excepción si la empresa del usuario actual está inactiva.
     * Super admins (sin company) son ignorados.
     */
    public void checkCompanyActivaDelUsuario() {
        checkCompanyActiva(SecurityUtils.getCurrentCompanyId());
    }
}
