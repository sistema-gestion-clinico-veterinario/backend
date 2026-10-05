package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleLoginExchangeStoreTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T12:00:00Z");

    private GoogleLoginExchangeStore store;

    @BeforeEach
    void setUp() {
        store = new GoogleLoginExchangeStore();
        pasarA(INICIO);
    }

    private void pasarA(Instant momento) {
        ReflectionTestUtils.setField(store, "clock", Clock.fixed(momento, ZoneOffset.UTC));
    }

    @Test
    void elCodigoDeCanjeSirveUnaSolaVez() {
        AuthResponse sesion = new AuthResponse();
        String code = store.store(sesion);

        assertThat(store.consume(code)).isSameAs(sesion);
        assertThat(store.consume(code)).isNull();
    }

    @Test
    void elCodigoVenceALosSesentaSegundos() {
        String vigente = store.store(new AuthResponse());
        String vencido = store.store(new AuthResponse());

        pasarA(INICIO.plus(Duration.ofSeconds(59)));
        assertThat(store.consume(vigente)).isNotNull();

        pasarA(INICIO.plus(Duration.ofSeconds(61)));
        assertThat(store.consume(vencido)).isNull();
    }

    @Test
    void uncodigoDesconocidoNoDevuelveNada() {
        assertThat(store.consume("no-existe")).isNull();
    }

    @Test
    void unCodigoVencidoSeRetiraAunqueNadieLoCanjee() {
        String vencido = store.store(new AuthResponse());
        pasarA(INICIO.plus(Duration.ofSeconds(120)));

        store.store(new AuthResponse());

        assertThat(store.consume(vencido)).isNull();
    }
}
