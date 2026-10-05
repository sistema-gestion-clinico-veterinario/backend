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

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveStaffMembership(Integer usuarioId, Integer companyId) {
        if (usuarioId == null || companyId == null) {
            return false;
        }
        return empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(usuarioId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyMembership(Integer usuarioId, Integer companyId) {
        if (usuarioId == null || companyId == null) {
            return false;
        }
        return empleadoRepository.existsByUserIdAndCompanyId(usuarioId, companyId)
                || apoderadoRepository.existsByUserIdAndCompanyId(usuarioId, companyId)
                || usuarioMembresiaRepository.existsByUsuarioIdAndCompanyId(usuarioId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSuspendedIn(Integer usuarioId, Integer companyId) {
        return isInactiveAs(usuarioId, companyId, veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeactivatedIn(Integer usuarioId, Integer companyId) {
        return isInactiveAs(usuarioId, companyId, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);
    }

    private boolean isInactiveAs(Integer usuarioId, Integer companyId,
                                 veterinaria.vargasvet.domain.enums.TipoInactividad tipo) {
        if (usuarioId == null || companyId == null) {
            return false;
        }
        return empleadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(usuarioId, companyId, tipo)
                || apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoFalseAndTipoInactividad(usuarioId, companyId, tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOnlyInactiveMemberships(Integer usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        boolean tieneRelaciones = empleadoRepository.existsByUserId(usuarioId)
                || apoderadoRepository.existsByUserId(usuarioId)
                || usuarioMembresiaRepository.existsByUsuarioId(usuarioId);
        if (!tieneRelaciones) {
            return false;
        }
        return !(empleadoRepository.existsByUserIdAndEstadoTrue(usuarioId)
                || apoderadoRepository.existsByUserIdAndEstadoTrue(usuarioId)
                || usuarioMembresiaRepository.existsByUsuarioIdAndEstadoTrue(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailTakenInUserCompanies(Usuario usuario, String email) {
        return takenByAnotherUser(usuario, usuarioRepository.findAllByEmailIgnoreCase(email));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameTakenInUserCompanies(Usuario usuario, String username) {
        return takenByAnotherUser(usuario, usuarioRepository.findAllByUsernameIgnoreCase(username));
    }

    private boolean takenByAnotherUser(Usuario usuario, java.util.List<Usuario> candidates) {
        Set<Integer> companyIds = getActiveCompanyIds(usuario);
        if (companyIds.isEmpty()) {
            return false;
        }
        return candidates.stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), usuario.getId()))
                .anyMatch(candidate -> companyIds.stream()
                        .anyMatch(companyId -> hasActiveMembership(candidate.getId(), companyId)));
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
        Integer currentCompanyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        Integer resolvedCompanyId = activeCompanyIds.size() == 1
                ? activeCompanyIds.iterator().next()
                : activeCompanyIds.isEmpty() ? currentCompanyId : null;
        if (Objects.equals(currentCompanyId, resolvedCompanyId)) {
            return;
        }
        usuario.setCompany(resolvedCompanyId == null ? null : companyRepository.getReferenceById(resolvedCompanyId));
        usuarioRepository.save(usuario);
    }
}
