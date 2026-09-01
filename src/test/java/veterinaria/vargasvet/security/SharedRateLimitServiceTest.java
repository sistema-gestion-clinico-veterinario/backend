package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import veterinaria.vargasvet.exception.RateLimitExceededException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SharedRateLimitServiceTest {

    @Test
    void storesOnlyHashedIdentifierAndAllowsWithinCapacity() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(2);
        SharedRateLimitService service = new SharedRateLimitService(jdbc);

        assertTrue(service.tryConsume("login-account", "persona@example.com", 3, Duration.ofMinutes(15)));

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(anyString(), eq(Integer.class), args.capture());
        assertEquals(64, args.getValue()[0].toString().length());
        assertFalse(args.getValue()[0].toString().contains("persona@example.com"));
    }

    @Test
    void rejectsWhenAtomicCounterExceedsCapacity() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(4);
        SharedRateLimitService service = new SharedRateLimitService(jdbc);

        assertThrows(RateLimitExceededException.class,
                () -> service.enforce("login-account", "persona@example.com", 3, Duration.ofMinutes(15)));
    }
}
