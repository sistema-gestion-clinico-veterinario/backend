package veterinaria.vargasvet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import veterinaria.vargasvet.repository.RefreshTokenRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Una conexión WebSocket abierta no sobrevive al cierre (o revocación) de la sesión que la autorizó. */
@ExtendWith(MockitoExtension.class)
class RealtimeSubscriptionGuardTest {

    @Mock RolePermissionEvaluator rolePermissionEvaluator;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock WebSocketSession abierta;
    @Mock WebSocketSession revocada;

    private RealtimeSubscriptionGuard guard;

    @BeforeEach
    void setUp() {
        guard = new RealtimeSubscriptionGuard(rolePermissionEvaluator, refreshTokenRepository);
    }

    private void conectar(WebSocketSession session, String webSocketId, String loginSessionId) {
        when(session.getId()).thenReturn(webSocketId);
        guard.trackSession(session);
        guard.bindSession(webSocketId, loginSessionId);
    }

    @Test
    void alCerrarSesionSeCierranSusConexionesAbiertasYSoloEsas() throws Exception {
        conectar(abierta, "ws-1", "sesion-a");
        conectar(revocada, "ws-2", "sesion-b");
        when(revocada.isOpen()).thenReturn(true);

        guard.closeConnectionsOfSession("sesion-b");

        verify(revocada).close(any(CloseStatus.class));
        verify(abierta, never()).close(any(CloseStatus.class));
    }

    @Test
    void laRevalidacionPeriodicaCierraLasConexionesDeSesionesRevocadas() throws Exception {
        conectar(abierta, "ws-1", "sesion-a");
        conectar(revocada, "ws-2", "sesion-b");
        when(revocada.isOpen()).thenReturn(true);
        when(refreshTokenRepository.findActiveFamilyIds(anyCollection())).thenReturn(List.of("sesion-a"));

        guard.revalidate();

        verify(revocada).close(any(CloseStatus.class));
        verify(abierta, never()).close(any(CloseStatus.class));
    }

    @Test
    void unaConexionSinSesionAsociadaNoSeTocaPorRevocacion() throws Exception {
        when(abierta.getId()).thenReturn("ws-viejo");
        guard.trackSession(abierta);
        guard.bindSession("ws-viejo", null);

        guard.revalidate();

        verify(abierta, never()).close(any(CloseStatus.class));
        verify(refreshTokenRepository, never()).findActiveFamilyIds(anyCollection());
    }
}
