package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.response.AssignedRoleResponse;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleLoginExchangeStoreTest {

    private static final Instant INICIO = Instant.parse("2026-10-05T12:00:00Z");

    private RepositorioEstadoOAuthEnMemoria repositorio;
    private GoogleLoginExchangeStore store;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioEstadoOAuthEnMemoria();
        store = new GoogleLoginExchangeStore(new EstadoOAuthCompartido(repositorio, new CifradoEstadoOAuth(new byte[32]), new ObjectMapper()));
        pasarA(INICIO);
    }

    private void pasarA(Instant momento) {
        repositorio.fijarReloj(momento);
    }

    @Test
    void elCodigoDeCanjeSirveUnaSolaVez() {
        AuthResponse sesion = new AuthResponse();
        String code = store.store(sesion);

        assertThat(store.consume(code)).isEqualTo(sesion);
        assertThat(store.consume(code)).isNull();
    }

    @Test
    void laSesionCanjeadaConservaLosTokensQueLaRespuestaNoSerializa() {
        AuthResponse sesion = new AuthResponse();
        sesion.setToken("token-de-acceso");
        sesion.setRefreshToken("token-de-refresco");
        sesion.setCompanyId(7);
        sesion.setCompanySlug("vargas-vet");
        sesion.setRoles(List.of("ROLE_ADMIN"));
        sesion.setAvailableRoles(List.of(AssignedRoleResponse.builder().id(3).name("ROLE_ADMIN")
                .scope(RoleScope.STAFF).purpose(RolePurpose.COMPANY_ADMIN).build()));
        sesion.setMenu(List.of(Map.of("codigo", "VISTA_CITAS", "hijos", List.of())));
        sesion.setPermissionVersion(12L);
        sesion.setPasswordChanged(true);

        AuthResponse canjeada = store.consume(store.store(sesion));

        assertThat(canjeada.getToken()).isEqualTo("token-de-acceso");
        assertThat(canjeada.getRefreshToken()).isEqualTo("token-de-refresco");
        assertThat(canjeada).usingRecursiveComparison().isEqualTo(sesion);
        assertThat(repositorio.volcado()).doesNotContain("token-de-acceso").doesNotContain("token-de-refresco");
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
        assertThat(store.consume(null)).isNull();
    }

    @Test
    void unCodigoVencidoSeRetiraAunqueNadieLoCanjee() {
        String vencido = store.store(new AuthResponse());
        pasarA(INICIO.plus(Duration.ofSeconds(120)));

        store.store(new AuthResponse());

        assertThat(repositorio.tamano()).isEqualTo(1);
        assertThat(store.consume(vencido)).isNull();
    }

    @Test
    void laReactivacionSeCanjeaUnaSolaVezYVenceALosCincoMinutos() {
        GoogleLoginExchangeStore.Reactivation datos = new GoogleLoginExchangeStore.Reactivation(5, 7, "ana@clinica.pe", "vargas-vet");
        String ticket = store.storeReactivation(datos);
        String vencido = store.storeReactivation(datos);

        assertThat(store.consumeReactivation(ticket)).isEqualTo(datos);
        assertThat(store.consumeReactivation(ticket)).isNull();

        pasarA(INICIO.plus(Duration.ofMinutes(6)));
        assertThat(store.consumeReactivation(vencido)).isNull();
    }

    @Test
    void unCodigoDeSesionNoSirveComoTicketDeReactivacion() {
        String code = store.store(new AuthResponse());

        assertThat(store.consumeReactivation(code)).isNull();
    }
}
