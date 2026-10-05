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
}
