package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.CodigoVerificacion;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.exception.InvalidVerificationCodeException;
import veterinaria.vargasvet.repository.CodigoVerificacionRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.util.AppClock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CodigoVerificacionRepository repository;

    @Value("${security.verification-code-validity-minutes:10}")
    private long validityMinutes;

    @Value("${security.verification-code-max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public String issue(Usuario usuario, Company company, String purpose) {
        repository.deletePendientes(usuario.getId(), company.getId(), purpose);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String salt = SecurityTokenUtils.generate().substring(0, 16);
        CodigoVerificacion row = new CodigoVerificacion();
        row.setUsuario(usuario);
        row.setCompany(company);
        row.setProposito(purpose);
        row.setSal(salt);
        row.setCodigoHash(hash(salt, code));
        row.setCreadoAt(AppClock.now());
        row.setExpiraAt(AppClock.now().plusMinutes(validityMinutes));
        repository.save(row);
        return code;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = InvalidVerificationCodeException.class)
    public void verifyAndConsume(Integer usuarioId, Integer companyId, String purpose, String code) {
        CodigoVerificacion row = repository
                .findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(usuarioId, companyId, purpose)
                .orElseThrow(() -> new InvalidVerificationCodeException("El código no es válido o venció. Pide uno nuevo."));
        if (row.getExpiraAt().isBefore(AppClock.now())) {
            throw new InvalidVerificationCodeException("El código venció. Pide uno nuevo.");
        }
        if (row.getIntentos() >= maxAttempts) {
            throw new InvalidVerificationCodeException("Superaste los intentos permitidos. Pide un código nuevo.");
        }
        boolean matches = code != null && MessageDigest.isEqual(
                hash(row.getSal(), code.trim()).getBytes(StandardCharsets.UTF_8),
                row.getCodigoHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            row.setIntentos(row.getIntentos() + 1);
            repository.save(row);
            throw new InvalidVerificationCodeException("El código no es correcto.");
        }
        row.setUsadoAt(AppClock.now());
        repository.save(row);
    }

    private static String hash(String salt, String code) {
        return SecurityTokenUtils.hash(salt + ":" + code);
    }
}
