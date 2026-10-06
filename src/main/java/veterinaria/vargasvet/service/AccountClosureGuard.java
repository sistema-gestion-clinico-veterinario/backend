package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CierreCuentaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;

@Component
@RequiredArgsConstructor
public class AccountClosureGuard {

    private final CierreCuentaRepository cierreCuentaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;

    public boolean hasOpenClosure(Integer usuarioId, Integer companyId) {
        return usuarioId != null && companyId != null
                && cierreCuentaRepository.existsByUsuarioIdAndCompanyIdAndEstado(
                        usuarioId, companyId, EstadoCierreCuenta.CERRADA);
    }

    public java.util.Optional<veterinaria.vargasvet.domain.entity.CierreCuenta> findReactivable(
            Integer usuarioId, Integer companyId) {
        if (usuarioId == null || companyId == null) {
            return java.util.Optional.empty();
        }
        return cierreCuentaRepository
                .findFirstByUsuarioIdAndCompanyIdAndEstadoOrderByCerradaAtDesc(usuarioId, companyId, EstadoCierreCuenta.CERRADA)
                .filter(cierre -> cierre.getVenceAt().isAfter(veterinaria.vargasvet.util.AppClock.now()))
                .filter(this::sigueCerradaPorSuDueno);
    }

    /** Solo se puede volver si el acceso quedó tal como lo dejó el cierre hecho por la propia persona:
     * una suspensión, una baja o un bloqueo decididos después por la clínica no se levantan iniciando sesión. */
    public boolean sigueCerradaPorSuDueno(veterinaria.vargasvet.domain.entity.CierreCuenta cierre) {
        if (cierre.getUsuario() == null || !cierre.getUsuario().isActivo()) {
            return false;
        }
        boolean empleadoDeBaja = cierre.getEmpleadoId() == null || empleadoRepository.findById(cierre.getEmpleadoId())
                .map(e -> Boolean.FALSE.equals(e.getEstado()) && e.getTipoInactividad() == TipoInactividad.BAJA)
                .orElse(false);
        boolean apoderadoDeBaja = cierre.getApoderadoId() == null || apoderadoRepository.findById(cierre.getApoderadoId())
                .map(a -> Boolean.FALSE.equals(a.getEstado()) && a.getTipoInactividad() == TipoInactividad.BAJA)
                .orElse(false);
        return empleadoDeBaja && apoderadoDeBaja;
    }

    public void assertNotSelfClosed(Integer usuarioId, Integer companyId) {
        if (hasOpenClosure(usuarioId, companyId)) {
            throw new IllegalArgumentException("La persona cerró su propia cuenta. Solo ella puede reactivarla "
                    + "iniciando sesión, mientras dure el plazo de reactivación.");
        }
    }
}
