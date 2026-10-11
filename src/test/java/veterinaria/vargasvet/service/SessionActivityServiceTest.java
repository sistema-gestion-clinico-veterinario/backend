package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.service.SessionActivityService.Estado;
import veterinaria.vargasvet.util.AppClock;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionActivityServiceTest {

    private static final String SESION = "sesion-1";
    private static final long LIMITE = 1800;
    private static final long INTERVALO = 60;

    @Mock RefreshTokenRepository refreshTokenRepository;

    private SessionActivityService service;

    @BeforeEach
    void setUp() {
        service = new SessionActivityService(refreshTokenRepository, LIMITE, INTERVALO);
    }

    private RefreshTokenRepository.ActividadDeSesion actividad(long abiertas, Instant ultima) {
        return new RefreshTokenRepository.ActividadDeSesion() {
            @Override public long getAbiertas() { return abiertas; }
            @Override public Instant getUltimaActividad() { return ultima; }
        };
    }

    private Instant haceSegundos(long segundos) {
        return AppClock.instantNow().minusSeconds(segundos);
    }

    @Test
    void unaSesionSinTokensAbiertosEstaCerrada() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(0, haceSegundos(5)));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.CERRADA);

        verify(refreshTokenRepository, never()).registrarActividad(anyString(), any());
    }

    @Test
    void pasadoElLimiteSinActividadLaSesionEstaInactivaYNoSeReactivaSola() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(1, haceSegundos(LIMITE + 5)));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.INACTIVA);

        verify(refreshTokenRepository, never()).registrarActividad(anyString(), any());
    }

    @Test
    void justoDentroDelLimiteLaSesionSigueActiva() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(1, haceSegundos(LIMITE - 5)));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.ACTIVA);
    }

    @Test
    void laActividadRecienteNoGeneraUnaEscrituraPorPeticion() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(1, haceSegundos(INTERVALO - 10)));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.ACTIVA);

        verify(refreshTokenRepository, never()).registrarActividad(anyString(), any());
    }

    @Test
    void pasadoElIntervaloLaActividadSeRegistra() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(1, haceSegundos(INTERVALO + 10)));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.ACTIVA);

        verify(refreshTokenRepository).registrarActividad(anyString(), any());
    }

    @Test
    void unaSesionAnteriorALaMedicionNoSeCierraYSeLeRegistraLaActividad() {
        when(refreshTokenRepository.resumenDeActividad(SESION)).thenReturn(actividad(1, null));

        assertThat(service.registrar(SESION)).isEqualTo(Estado.ACTIVA);

        verify(refreshTokenRepository).registrarActividad(anyString(), any());
    }

    @Test
    void estaInactivaSoloCuandoLaUltimaActividadDeLaSesionSuperaElLimite() {
        when(refreshTokenRepository.ultimaActividadDeLaSesion("vieja")).thenReturn(haceSegundos(LIMITE + 5));
        when(refreshTokenRepository.ultimaActividadDeLaSesion("reciente")).thenReturn(haceSegundos(LIMITE - 5));
        when(refreshTokenRepository.ultimaActividadDeLaSesion("sin-dato")).thenReturn(null);

        assertThat(service.estaInactiva("vieja")).isTrue();
        assertThat(service.estaInactiva("reciente")).isFalse();
        assertThat(service.estaInactiva("sin-dato")).isFalse();
        assertThat(service.estaInactiva(null)).isFalse();
    }

    @Test
    void laConfiguracionInvalidaImpideArrancar() {
        assertThatThrownBy(() -> new SessionActivityService(refreshTokenRepository, 0, 0))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new SessionActivityService(refreshTokenRepository, 600, 600))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exponeElLimiteConfigurado() {
        assertThat(service.limiteDeInactividadEnSegundos()).isEqualTo(LIMITE);
    }
}
