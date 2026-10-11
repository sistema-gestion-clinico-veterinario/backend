package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountLockoutServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks AccountLockoutService service;

    @Test
    void clearLockoutBorraElContadorDeEseIdentificador() {
        service.clearLockout("Ana.Perez");

        verify(jdbcTemplate).update(eq("DELETE FROM account_lockouts WHERE account_key = ?"), anyString());
    }

    @Test
    void clearLockoutIgnoraIdentificadoresVaciosParaNoTocarElContadorDeDesconocidos() {
        service.clearLockout(null);
        service.clearLockout("   ");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void antesDeConsultarElBloqueoEliminaUnBloqueoYaVencido() {
        service.assertNotLocked("company:7:ana");

        verify(jdbcTemplate).update(
                eq("DELETE FROM account_lockouts WHERE account_key = ? "
                        + "AND locked_until IS NOT NULL AND locked_until <= CURRENT_TIMESTAMP"),
                anyString());
        verify(jdbcTemplate).query(eq("SELECT locked_until FROM account_lockouts WHERE account_key = ?"),
                any(org.springframework.jdbc.core.ResultSetExtractor.class), anyString());
    }
}
