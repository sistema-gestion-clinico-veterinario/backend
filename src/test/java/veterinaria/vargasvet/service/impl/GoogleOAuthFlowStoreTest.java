package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.exception.RateLimitExceededException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOAuthFlowStoreTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T12:00:00Z");

    private GoogleOAuthFlowStore store;

    @BeforeEach
    void setUp() {
        store = new GoogleOAuthFlowStore();
        pasarA(INICIO);
    }

    private void pasarA(Instant momento) {
        ReflectionTestUtils.setField(store, "clock", Clock.fixed(momento, ZoneOffset.UTC));
    }

    private GoogleOAuthFlowStore.Context login() {
        return new GoogleOAuthFlowStore.Context("vargas-vet", null);
    }

    @Test
    void elCodigoDeIntencionSeCanjeaUnaSolaVezYDevuelveElContexto() {
        String intent = store.createIntent(login());

        assertThat(store.consumeIntent(intent)).isEqualTo(login());
        assertThat(store.consumeIntent(intent)).isNull();
    }

    @Test
    void unaActivacionConservaElTokenDeInvitacionSoloEnElServidor() {
        String intent = store.createIntent(new GoogleOAuthFlowStore.Context(null, "token-de-invitacion"));

        GoogleOAuthFlowStore.Context contexto = store.consumeIntent(intent);

        assertThat(contexto.isActivation()).isTrue();
        assertThat(contexto.activationToken()).isEqualTo("token-de-invitacion");
        assertThat(intent).doesNotContain("token-de-invitacion");
    }

    @Test
    void codigosDesconocidosOVaciosNoDevuelvenNada() {
        assertThat(store.consumeIntent("no-existe")).isNull();
        assertThat(store.consumeIntent("  ")).isNull();
        assertThat(store.consumeIntent(null)).isNull();
        assertThat(store.consumeAuthorization("no-existe")).isNull();
        assertThat(store.consumeAuthorization(null)).isNull();
    }

    @Test
    void laIntencionVenceALosCincoMinutos() {
        String vigente = store.createIntent(login());
        String vencida = store.createIntent(login());

        pasarA(INICIO.plus(Duration.ofMinutes(4)));
        assertThat(store.consumeIntent(vigente)).isNotNull();

        pasarA(INICIO.plus(Duration.ofMinutes(6)));
        assertThat(store.consumeIntent(vencida)).isNull();
    }

    @Test
    void laAutorizacionSeCanjeaUnaSolaVezYConservaElVerificadorPkce() {
        String state = store.createAuthorization(new GoogleOAuthFlowStore.Authorization(login(), "verificador"));

        GoogleOAuthFlowStore.Authorization autorizacion = store.consumeAuthorization(state);

        assertThat(autorizacion.codeVerifier()).isEqualTo("verificador");
        assertThat(autorizacion.context()).isEqualTo(login());
        assertThat(store.consumeAuthorization(state)).isNull();
    }

    @Test
    void laAutorizacionVenceALosDiezMinutos() {
        String state = store.createAuthorization(new GoogleOAuthFlowStore.Authorization(login(), "v"));

        pasarA(INICIO.plus(Duration.ofMinutes(11)));

        assertThat(store.consumeAuthorization(state)).isNull();
    }

    @Test
    void cadaCodigoEsDistintoYImposibleDeAdivinar() {
        String uno = store.createIntent(login());
        String dos = store.createIntent(login());

        assertThat(uno).isNotEqualTo(dos).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void unaIntencionNoSirveComoAutorizacionNiAlReves() {
        String intent = store.createIntent(login());
        String state = store.createAuthorization(new GoogleOAuthFlowStore.Authorization(login(), "v"));

        assertThat(store.consumeAuthorization(intent)).isNull();
        assertThat(store.consumeIntent(state)).isNull();
    }

    @Test
    void sinEspacioNoAceptaMasSolicitudesPeroLasVencidasLiberanLugar() {
        for (int i = 0; i < 10_000; i++) {
            store.createIntent(login());
        }

        assertThatThrownBy(() -> store.createIntent(login())).isInstanceOf(RateLimitExceededException.class);

        pasarA(INICIO.plus(Duration.ofMinutes(6)));
        assertThat(store.createIntent(login())).isNotBlank();
    }
}
