package veterinaria.vargasvet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.time.Instant;

/**
 * Mide la inactividad de una sesión en el servidor. Cada petición autenticada la registra, de modo que el límite
 * no depende de lo que haga el navegador ni de cuánto dure el token de acceso.
 */
@Service
public class SessionActivityService {

    public enum Estado { ACTIVA, CERRADA, INACTIVA }

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration limiteDeInactividad;
    private final Duration intervaloDeRegistro;

    public SessionActivityService(RefreshTokenRepository refreshTokenRepository,
                                  @Value("${jwt.idle-timeout-seconds:1800}") long idleTimeoutSeconds,
                                  @Value("${jwt.activity-touch-interval-seconds:60}") long touchIntervalSeconds) {
        if (idleTimeoutSeconds <= 0) {
            throw new IllegalStateException("jwt.idle-timeout-seconds debe ser mayor que cero");
        }
        if (touchIntervalSeconds < 0 || touchIntervalSeconds >= idleTimeoutSeconds) {
            throw new IllegalStateException(
                    "jwt.activity-touch-interval-seconds debe ser menor que jwt.idle-timeout-seconds");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.limiteDeInactividad = Duration.ofSeconds(idleTimeoutSeconds);
        this.intervaloDeRegistro = Duration.ofSeconds(touchIntervalSeconds);
    }

    /**
     * Se llama en cada petición autenticada. Si la sesión sigue abierta y dentro del límite, anota la actividad
     * (como mucho una escritura por intervalo, para no cargar la base con cada petición).
     */
    @Transactional
    public Estado registrar(String sessionId) {
        RefreshTokenRepository.ActividadDeSesion sesion = refreshTokenRepository.resumenDeActividad(sessionId);
        if (sesion.getAbiertas() == 0) {
            return Estado.CERRADA;
        }
        Instant ahora = AppClock.instantNow();
        Instant ultima = sesion.getUltimaActividad();
        if (ultima != null && superaElLimite(ultima, ahora)) {
            return Estado.INACTIVA;
        }
        if (ultima == null || ultima.plus(intervaloDeRegistro).isBefore(ahora)) {
            refreshTokenRepository.registrarActividad(sessionId, ahora);
        }
        return Estado.ACTIVA;
    }

    /** ¿La sesión superó el límite desde su última actividad? Una fila sin actividad registrada no se considera vencida. */
    @Transactional(readOnly = true)
    public boolean estaInactiva(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Instant ultima = refreshTokenRepository.ultimaActividadDeLaSesion(sessionId);
        return ultima != null && superaElLimite(ultima, AppClock.instantNow());
    }

    public long limiteDeInactividadEnSegundos() {
        return limiteDeInactividad.toSeconds();
    }

    private boolean superaElLimite(Instant ultima, Instant ahora) {
        return ultima.plus(limiteDeInactividad).isBefore(ahora);
    }
}
