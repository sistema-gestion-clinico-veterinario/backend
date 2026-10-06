package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.repository.CierreCuentaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountClosureGuardTest {

    @Mock CierreCuentaRepository repository;
    @Mock veterinaria.vargasvet.repository.EmpleadoRepository empleadoRepository;
    @Mock veterinaria.vargasvet.repository.ApoderadoRepository apoderadoRepository;
    @InjectMocks AccountClosureGuard guard;

    @Test
    void unCierreAbiertoImpideQueOtroLaReactive() {
        when(repository.existsByUsuarioIdAndCompanyIdAndEstado(10, 7, EstadoCierreCuenta.CERRADA)).thenReturn(true);

        assertThat(guard.hasOpenClosure(10, 7)).isTrue();
        assertThatThrownBy(() -> guard.assertNotSelfClosed(10, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cerró su propia cuenta")
                .hasMessageContaining("Solo ella puede reactivarla");
    }

    @Test
    void sinCierreAbiertoNoHayRestriccion() {
        when(repository.existsByUsuarioIdAndCompanyIdAndEstado(10, 7, EstadoCierreCuenta.CERRADA)).thenReturn(false);

        assertThatCode(() -> guard.assertNotSelfClosed(10, 7)).doesNotThrowAnyException();
    }

    @Test
    void elCierreEnUnaClinicaNoRestringeAOtra() {
        when(repository.existsByUsuarioIdAndCompanyIdAndEstado(10, 8, EstadoCierreCuenta.CERRADA)).thenReturn(false);

        assertThat(guard.hasOpenClosure(10, 8)).isFalse();
    }

    @Test
    void sinIdentificadoresNoSeConsultaNada() {
        assertThat(guard.hasOpenClosure(null, 7)).isFalse();
        assertThat(guard.hasOpenClosure(10, null)).isFalse();
        verifyNoInteractions(repository);
    }

    private veterinaria.vargasvet.domain.entity.CierreCuenta cierre(boolean usuarioActivo, java.time.LocalDateTime vence) {
        veterinaria.vargasvet.domain.entity.Usuario usuario = new veterinaria.vargasvet.domain.entity.Usuario();
        usuario.setId(10);
        usuario.setActivo(usuarioActivo);
        veterinaria.vargasvet.domain.entity.CierreCuenta cierre = new veterinaria.vargasvet.domain.entity.CierreCuenta();
        cierre.setUsuario(usuario);
        cierre.setApoderadoId(5L);
        cierre.setVenceAt(vence);
        when(repository.findFirstByUsuarioIdAndCompanyIdAndEstadoOrderByCerradaAtDesc(10, 7, EstadoCierreCuenta.CERRADA))
                .thenReturn(java.util.Optional.of(cierre));
        return cierre;
    }

    private void apoderado(Boolean estado, veterinaria.vargasvet.domain.enums.TipoInactividad tipo) {
        veterinaria.vargasvet.domain.entity.Apoderado apoderado = new veterinaria.vargasvet.domain.entity.Apoderado();
        apoderado.setEstado(estado);
        apoderado.setTipoInactividad(tipo);
        when(apoderadoRepository.findById(5L)).thenReturn(java.util.Optional.of(apoderado));
    }

    private java.time.LocalDateTime enElFuturo() {
        return veterinaria.vargasvet.util.AppClock.now().plusDays(10);
    }

    @Test
    void quienCerroSuPropiaCuentaDentroDelPlazoPuedeReactivarla() {
        cierre(true, enElFuturo());
        apoderado(false, veterinaria.vargasvet.domain.enums.TipoInactividad.BAJA);

        assertThat(guard.findReactivable(10, 7)).isPresent();
    }

    @Test
    void pasadoElPlazoYaNoSePuedeReactivar() {
        cierre(true, veterinaria.vargasvet.util.AppClock.now().minusDays(1));

        assertThat(guard.findReactivable(10, 7)).isEmpty();
    }

    @Test
    void unaCuentaSuspendidaPorLaClinicaDespuesDelCierreNoSeReactivaIniciandoSesion() {
        cierre(true, enElFuturo());
        apoderado(false, veterinaria.vargasvet.domain.enums.TipoInactividad.SUSPENSION);

        assertThat(guard.findReactivable(10, 7)).isEmpty();
    }

    @Test
    void siElUsuarioFueDesactivadoPorLaPlataformaNoSeReactiva() {
        cierre(false, enElFuturo());

        assertThat(guard.findReactivable(10, 7)).isEmpty();
    }

    @Test
    void siLaPersonaYaFueReactivadaPorOtraViaNoHayNadaQueReactivar() {
        cierre(true, enElFuturo());
        apoderado(true, null);

        assertThat(guard.findReactivable(10, 7)).isEmpty();
    }

    @Test
    void sinCierreNoHayNadaQueReactivar() {
        when(repository.findFirstByUsuarioIdAndCompanyIdAndEstadoOrderByCerradaAtDesc(10, 7, EstadoCierreCuenta.CERRADA))
                .thenReturn(java.util.Optional.empty());

        assertThat(guard.findReactivable(10, 7)).isEmpty();
    }
}
