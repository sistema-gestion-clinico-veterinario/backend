package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import veterinaria.vargasvet.domain.entity.RealtimeTicket;
import veterinaria.vargasvet.repository.RealtimeTicketRepository;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealtimeTicketServiceTest {
    @Mock TokenProvider tokenProvider;
    @Mock RealtimeTicketRepository repository;

    @Test
    void registraElJtiAlEmitirTicket() {
        RealtimeTicketService service = new RealtimeTicketService(tokenProvider, repository);
        Authentication authentication = authentication();
        Instant now = Instant.parse("2026-08-27T20:00:00Z");
        when(tokenProvider.createRealtimeTicket(eq(7), eq("vet@example.com"), anyList(), eq(3)))
                .thenReturn(new TokenProvider.IssuedRealtimeTicket(
                        "signed-ticket", "ticket-jti", now, now.plusSeconds(60)));

        assertEquals("signed-ticket", service.issue(authentication));

        ArgumentCaptor<RealtimeTicket> saved = ArgumentCaptor.forClass(RealtimeTicket.class);
        verify(repository).save(saved.capture());
        assertEquals("ticket-jti", saved.getValue().getJti());
        assertEquals(7, saved.getValue().getUsuarioId());
    }

    @Test
    void unTicketConsumidoNoPuedeReutilizarse() {
        RealtimeTicketService service = new RealtimeTicketService(tokenProvider, repository);
        Authentication authentication = authentication();
        TokenProvider.RealtimeTicketDetails details = new TokenProvider.RealtimeTicketDetails(
                authentication, "ticket-jti", Instant.now().plusSeconds(60));
        when(tokenProvider.getRealtimeTicketDetails("signed-ticket")).thenReturn(details);
        when(repository.consumeOnce(eq("ticket-jti"), any(Instant.class))).thenReturn(1, 0);

        assertTrue(service.consume("signed-ticket").isPresent());
        assertTrue(service.consume("signed-ticket").isEmpty());
    }

    private Authentication authentication() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var principal = new UsuarioPrincipal(7, "vet@example.com", "", authorities, 3);
        return new UsernamePasswordAuthenticationToken(principal, "token", authorities);
    }
}
