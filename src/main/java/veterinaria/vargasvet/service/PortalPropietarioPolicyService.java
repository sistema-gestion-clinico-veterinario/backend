package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.EstadoAccesoPortalPropietario;
import veterinaria.vargasvet.domain.enums.ModoAccesoPropietario;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;


@Service
@RequiredArgsConstructor
public class PortalPropietarioPolicyService {

    private final ApoderadoRepository apoderadoRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessionSecurityService sessionSecurityService;

    @Transactional
    public int aplicarCambio(Company company, ModoAccesoPropietario anterior,
                             ModoAccesoPropietario nuevo) {
        if (nuevo != ModoAccesoPropietario.SOLO_GESTION_INTERNA
                || anterior == ModoAccesoPropietario.SOLO_GESTION_INTERNA) {
            return 0;
        }

        Integer companyId = company.getId();
        int revocados = apoderadoRepository.cambiarEstadoAccesoPortalDeLaEmpresa(companyId,
                EstadoAccesoPortalPropietario.REVOCADO, AppClock.now(), SecurityUtils.getCurrentUserEmail());
        sessionSecurityService.invalidateClientSessionsForCompany(company);
        passwordResetTokenRepository.deleteDeLosApoderadosDeLaEmpresa(companyId);
        usuarioRepository.limpiarTokenDeVerificacionDeApoderadosSinRol(companyId, RoleScope.STAFF);
        return revocados;
    }
}
