package veterinaria.vargasvet.ers.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_notificaciones;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class NotificacionClaveInvalidaIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private ObjectMapper json;

    private DosEmpresasSembradas.Empresa clinica;
    private Cookie[] sesion;

    @BeforeAll
    void abrirSesion() throws Exception {
        clinica = sembrador.sembrar("TKN");
        MvcResult inicio = mvc.perform(peticion(HttpMethod.POST, "/auth/login", null)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + clinica.slug() + "\",\"username\":\"admin.tkn\",\"password\":\""
                        + DosEmpresasSembradas.CLAVE + "\"}")).andReturn();
        assertThat(inicio.getResponse().getStatus()).as("inicio de sesión").isEqualTo(200);
        sesion = inicio.getResponse().getCookies();
    }

    @Test
    void unaClaveDeNotificacionNoReconocidaRespondeSolicitudIncorrecta() throws Exception {
        MvcResult respuesta = mvc.perform(peticion(HttpMethod.PUT, "/notifications/1/read", sesion)).andReturn();

        assertThat(respuesta.getResponse().getStatus()).isEqualTo(400);
        JsonNode cuerpo = json.readTree(respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(cuerpo.path("success").asBoolean()).isFalse();
        assertThat(cuerpo.path("message").asText()).isEqualTo("Notificación no reconocida");
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] cookies) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta)
                .header("Origin", "http://localhost:4200")
                .header("X-Company-Slug", clinica.slug());
        return cookies == null ? peticion : peticion.cookie(cookies);
    }
}
