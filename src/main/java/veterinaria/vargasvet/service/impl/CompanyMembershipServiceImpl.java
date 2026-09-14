package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.CompanyMembershipService;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyMembershipServiceImpl implements CompanyMembershipService {

    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioMembresiaRepository usuarioMembresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> getActiveCompanyIds(Usuario usuario) {
        Set<Integer> companyIds = new LinkedHashSet<>();
        empleadoRepository.findActiveByUserId(usuario.getId())
                .map(empleado -> empleado.getCompany())
                .ifPresent(company -> companyIds.add(company.getId()));
        apoderadoRepository.findAllActiveByUserId(usuario.getId()).forEach(apoderado -> {
            if (apoderado.getCompany() != null) {
                companyIds.add(apoderado.getCompany().getId());
            }
        });
        usuarioMembresiaRepository.findAllActiveByUsuarioId(usuario.getId()).forEach(membresia -> {
            if (membresia.getCompany() != null) {
                companyIds.add(membresia.getCompany().getId());
            }
        });
        return companyIds;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(Integer usuarioId, Integer companyId) {
        if (usuarioId == null || companyId == null) {
            return false;
        }
        return empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(usuarioId, companyId)
                || apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(usuarioId, companyId)
                || usuarioMembresiaRepository.existsByUsuarioIdAndCompanyIdAndEstadoTrue(usuarioId, companyId);
    }

    /**
     * REQUIRED (por defecto): se une a la transaccion del que llama en vez de abrir
     * una propia. Es asi como se cumple la regla de "misma transaccion que la
     * membresia" - el llamador ya debe estar dentro de un @Transactional que crea
     * o modifica la membresia; este metodo nunca hace su propio commit aparte.
     */
    @Override
    @Transactional
    public void syncLegacyCompanyField(Usuario usuario) {
        Set<Integer> activeCompanyIds = getActiveCompanyIds(usuario);
        Integer resolvedCompanyId = activeCompanyIds.size() == 1
                ? activeCompanyIds.iterator().next()
                : null;
        Integer currentCompanyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        if (Objects.equals(currentCompanyId, resolvedCompanyId)) {
            return;
        }
        usuario.setCompany(resolvedCompanyId == null ? null : companyRepository.getReferenceById(resolvedCompanyId));
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertNoActiveEmploymentElsewhere(Usuario usuario) {
        if (empleadoRepository.existsByUserIdAndEstadoTrue(usuario.getId())) {
            throw new IllegalArgumentException(
                    "Esta persona ya tiene una relación laboral activa registrada en el sistema. "
                            + "Para transferirla, la empresa donde trabaja actualmente debe darla de baja primero.");
        }
    }
}
