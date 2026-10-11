package veterinaria.vargasvet.ers.seguridad;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import veterinaria.vargasvet.exception.SessionInactivityException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-03. Con la aplicación completa y sesiones reales, comprueba que el servidor cierra la sesión tras 30 minutos sin
 * actividad aunque el navegador conserve las cookies, y que las cookies de sesión salen con sus atributos de seguridad.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_inactividad;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class InactividadDeSesionIntegrationTest {

    private static final Duration LIMITE = Duration.ofMinutes(30);

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DosEmpresasSembradas sembrador;

    private DosEmpresasSembradas.Empresa clinica;
    private Cookie[] cookies;
    private MvcResult inicioDeSesion;

    @BeforeAll
    void sembrarClinica() {
        clinica = sembrador.sembrar("TKI");
    }

    @BeforeEach
    void abrirSesion() throws Exception {
        jdbc.update("UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE revoked_at IS NULL");
        inicioDeSesion = mvc.perform(peticion(HttpMethod.POST, "/auth/login", null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + clinica.slug() + "\",\"username\":\"admin.tki\",\"password\":\""
                                + DosEmpresasSembradas.CLAVE + "\"}"))
                .andReturn();
        assertThat(inicioDeSesion.getResponse().getStatus()).as("inicio de sesión").isEqualTo(200);
        cookies = inicioDeSesion.getResponse().getCookies();
    }

    @Test
    void lasCookiesDeSesionSalenInaccesiblesParaScriptsYElCuerpoNoLlevaLosTokens() throws Exception {
        List<String> establecidas = inicioDeSesion.getResponse().getHeaders(HttpHeaders.SET_COOKIE);

        assertThat(establecidas).hasSize(2).allSatisfy(c -> assertThat(c).contains("HttpOnly").contains("SameSite=Lax"));
        String cuerpo = inicioDeSesion.getResponse().getContentAsString();
        for (Cookie cookie : cookies) {
            assertThat(cuerpo).as("el cuerpo de la respuesta no repite " + cookie.getName()).doesNotContain(cookie.getValue());
        }
        assertThat(cuerpo).doesNotContain("\"token\"").doesNotContain("refreshToken");
    }

    @Test
    void dentroDelLimiteLaSesionFuncionaYCadaPeticionRenuevaElPlazo() throws Exception {
        envejecer(Duration.ofMinutes(29));
        Instant antes = ultimaActividad();

        assertThat(estadoDe(HttpMethod.GET, "/auth/session")).isEqualTo(200);

        assertThat(ultimaActividad()).isAfter(antes.plusSeconds(60));
        assertThat(ultimaActividad()).isAfter(Instant.now().minus(Duration.ofMinutes(1)));
    }

    @Test
    void pasadoElLimiteLaApiRechazaLaSesionAunqueElNavegadorConserveLasCookies() throws Exception {
        envejecer(LIMITE.plusMinutes(1));

        assertThat(estadoDe(HttpMethod.GET, "/auth/session")).isEqualTo(401);
    }

    @Test
    void unaSesionInactivaTampocoSePuedeRenovarConLaCookieDeRefresh() throws Exception {
        envejecer(LIMITE.plusMinutes(1));

        MvcResult renovacion = mvc.perform(peticion(HttpMethod.POST, "/auth/refresh", cookies)).andReturn();

        assertThat(renovacion.getResponse().getStatus()).isEqualTo(401);
        assertThat(renovacion.getResponse().getContentAsString()).contains(SessionInactivityException.CODE);
        assertThat(renovacion.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NULL", Integer.class))
                .as("la sesión queda revocada").isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'SESION_EXPIRADA_INACTIVIDAD'", Integer.class))
                .as("queda registro en la auditoría").isEqualTo(1);
    }

    @Test
    void unaSesionConActividadRecienteSiSePuedeRenovarYElNuevoTokenEmpiezaSuPlazo() throws Exception {
        envejecer(Duration.ofMinutes(29));

        MvcResult renovacion = mvc.perform(peticion(HttpMethod.POST, "/auth/refresh", cookies)).andReturn();

        assertThat(renovacion.getResponse().getStatus()).isEqualTo(200);
        assertThat(ultimaActividad()).isAfter(Instant.now().minus(Duration.ofMinutes(1)));
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] sesion) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta)
                .header("Origin", "http://localhost:4200")
                .header("X-Company-Slug", clinica.slug());
        return sesion == null ? peticion : peticion.cookie(sesion);
    }

    private int estadoDe(HttpMethod verbo, String ruta) throws Exception {
        return mvc.perform(peticion(verbo, ruta, cookies)).andReturn().getResponse().getStatus();
    }

    private void envejecer(Duration tiempo) {
        jdbc.update("UPDATE refresh_tokens SET last_activity_at = ? WHERE revoked_at IS NULL",
                Timestamp.from(Instant.now().minus(tiempo)));
    }

    private Instant ultimaActividad() {
        return jdbc.queryForObject("SELECT MAX(last_activity_at) FROM refresh_tokens WHERE revoked_at IS NULL", Timestamp.class)
                .toInstant();
    }
}
