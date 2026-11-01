package veterinaria.vargasvet.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    private static final String BLOQUEAR = "UPDATE account_lockouts SET locked_until = ? WHERE account_key = ?";
    private static final String CONSULTAR = "SELECT locked_until FROM account_lockouts WHERE account_key = ?";

    private void intentosAcumulados(int intentos) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(intentos);
    }

    @Test
    @DisplayName("Al llegar al máximo de intentos fallidos la cuenta queda bloqueada por el tiempo configurado")
    void alLlegarAlMaximoDeIntentosLaCuentaQuedaBloqueada() {
        intentosAcumulados(5);
        Instant antes = Instant.now();

        service.registerFailedAttempt("ana@clinica.test");

        ArgumentCaptor<Timestamp> hasta = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbcTemplate).update(eq(BLOQUEAR), hasta.capture(), anyString());
        assertThat(hasta.getValue().toInstant())
                .isAfterOrEqualTo(antes.plus(Duration.ofMinutes(30)))
                .isBefore(Instant.now().plus(Duration.ofMinutes(30)).plusSeconds(5));
    }

    @Test
    @DisplayName("Con menos intentos que el máximo la cuenta no se bloquea")
    void conMenosIntentosQueElMaximoNoSeBloquea() {
        intentosAcumulados(4);

        service.registerFailedAttempt("ana@clinica.test");

        verify(jdbcTemplate, never()).update(eq(BLOQUEAR), any(Timestamp.class), anyString());
    }

    @Test
    @DisplayName("Si no se pudo contar el intento no se bloquea a nadie por error")
    void sinConteoNoSeBloquea() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn((Integer) null);

        service.registerFailedAttempt("ana@clinica.test");

        verify(jdbcTemplate, never()).update(eq(BLOQUEAR), any(Timestamp.class), anyString());
    }

    @Test
    @DisplayName("El máximo y la duración del bloqueo salen de la configuración")
    void respetaElMaximoYLaDuracionConfigurados() {
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 3);
        ReflectionTestUtils.setField(service, "lockoutMinutes", 10);
        intentosAcumulados(3);
        Instant antes = Instant.now();

        service.registerFailedAttempt("ana@clinica.test");

        ArgumentCaptor<Timestamp> hasta = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbcTemplate).update(eq(BLOQUEAR), hasta.capture(), anyString());
        assertThat(hasta.getValue().toInstant())
                .isAfterOrEqualTo(antes.plus(Duration.ofMinutes(10)))
                .isBefore(antes.plus(Duration.ofMinutes(11)));
    }

    @Test
    @DisplayName("Un mismo correo en mayúsculas o con espacios cuenta como la misma cuenta")
    void elIdentificadorSeNormalizaParaContarSobreLaMismaCuenta() {
        intentosAcumulados(5);

        service.registerFailedAttempt("Ana@Clinica.Test");
        service.registerFailedAttempt("  ana@clinica.test ");

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(eq(BLOQUEAR), any(Timestamp.class), claves.capture());
        assertThat(claves.getAllValues().get(0)).isEqualTo(claves.getAllValues().get(1));
    }

    @Test
    @DisplayName("Una cuenta con bloqueo vigente rechaza el acceso con el mismo mensaje que una contraseña incorrecta")
    void unBloqueoVigenteRechazaElAcceso() {
        when(jdbcTemplate.query(eq(CONSULTAR), ArgumentMatchers.<ResultSetExtractor<Timestamp>>any(), anyString()))
                .thenReturn(Timestamp.from(Instant.now().plus(Duration.ofMinutes(5))));

        assertThatThrownBy(() -> service.assertNotLocked("ana@clinica.test"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    @DisplayName("Sin bloqueo, o con el bloqueo ya vencido, la cuenta puede iniciar sesión")
    void sinBloqueoOConBloqueoVencidoSePermiteElAcceso() {
        when(jdbcTemplate.query(eq(CONSULTAR), ArgumentMatchers.<ResultSetExtractor<Timestamp>>any(), anyString()))
                .thenReturn(null, Timestamp.from(Instant.now().minusSeconds(30)));

        assertThatCode(() -> service.assertNotLocked("ana@clinica.test")).doesNotThrowAnyException();
        assertThatCode(() -> service.assertNotLocked("ana@clinica.test")).doesNotThrowAnyException();
    }
}
