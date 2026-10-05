package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.repository.CierreCuentaRepository;

@Component
@RequiredArgsConstructor
public class AccountClosureGuard {

    private final CierreCuentaRepository cierreCuentaRepository;

    public boolean hasOpenClosure(Integer usuarioId, Integer companyId) {
        return usuarioId != null && companyId != null
                && cierreCuentaRepository.existsByUsuarioIdAndCompanyIdAndEstado(
                        usuarioId, companyId, EstadoCierreCuenta.CERRADA);
    }

    public void assertNotSelfClosed(Integer usuarioId, Integer companyId) {
        if (hasOpenClosure(usuarioId, companyId)) {
            throw new IllegalArgumentException("La persona cerró su propia cuenta. Solo ella puede reactivarla "
                    + "desde el enlace que recibió por correo, mientras dure el plazo de reactivación.");
        }
    }
}
