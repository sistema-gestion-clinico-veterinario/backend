package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.EstadoAccesoPortalPropietario;
import veterinaria.vargasvet.domain.enums.ModoAccesoPropietario;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;


@Service
@RequiredArgsConstructor
public class PortalPropietarioPolicyService {

    private final ApoderadoRepository apoderadoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessionSecurityService sessionSecurityService;

    @Transactional
    public int aplicarCambio(Company company, ModoAccesoPropietario anterior,
                             ModoAccesoPropietario nuevo) {
        if (nuevo != ModoAccesoPropietario.SOLO_GESTION_INTERNA
                || anterior == ModoAccesoPropietario.SOLO_GESTION_INTERNA) {
            return 0;
        }

        int revocados = 0;
        for (Apoderado apoderado : apoderadoRepository.findByCompanyId(company.getId())) {
            apoderado.setEstadoAccesoPortal(EstadoAccesoPortalPropietario.REVOCADO);
            apoderado.setAccesoPortalModificadoEn(AppClock.now());
            apoderado.setAccesoPortalModificadoPor(SecurityUtils.getCurrentUserEmail());
            apoderadoRepository.save(apoderado);

            var usuario = apoderado.getUser();
            if (usuario == null) {
                revocados++;
                continue;
            }
            sessionSecurityService.invalidateSessionsForCompany(usuario, company);
            passwordResetTokenRepository.deleteByUsuarioAndCompany(usuario, company);
            boolean conservaAccesoLaboral = usuario.getId() != null
                    && usuarioPorRolRepository.existsActiveRoleScope(
                            usuario.getId(), company.getId(), RoleScope.STAFF);
            if (!conservaAccesoLaboral) {
                usuario.setVerificationToken(null);
                usuario.setVerificationTokenExpiresAt(null);
                usuarioRepository.save(usuario);
            }
            revocados++;
        }
        return revocados;
    }
}
