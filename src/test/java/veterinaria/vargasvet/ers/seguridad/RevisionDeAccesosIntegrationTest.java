package veterinaria.vargasvet.ers.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-36. La revisión de accesos privilegiados se ejecuta con sesiones reales: solo quien administra roles puede
 * consultarla o registrarla, cubre únicamente a su empresa y deja evidencia consultable que otra empresa no ve.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_revision_accesos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class RevisionDeAccesosIntegrationTest {

    private static final String RUTA = "/admin/access-reviews";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private DosEmpresasSembradas sembrador;

    private DosEmpresasSembradas.Empresa p;
    private DosEmpresasSembradas.Empresa q;
    private Cookie[] adminP;
    private Cookie[] adminQ;
    private Cookie[] sinPermisosP;
    private Cookie[] clienteP;

    @BeforeAll
    void prepararEscenario() throws Exception {
        p = sembrador.sembrar("RVP");
        q = sembrador.sembrar("RVQ");
        adminP = iniciarSesion(p.slug(), "admin.rvp");
        adminQ = iniciarSesion(q.slug(), "admin.rvq");
        sinPermisosP = iniciarSesion(p.slug(), "sinpermisos.rvp");
        clienteP = iniciarSesion(p.slug(), "cliente1.rvp");
    }

    @Test
    @Order(1)
    void quienNoAdministraRolesNoPuedeConsultarNiRegistrarLaRevision() throws Exception {
        assertThat(estado(HttpMethod.GET, RUTA, sinPermisosP, p.slug(), null)).isEqualTo(403);
        assertThat(estado(HttpMethod.POST, RUTA, sinPermisosP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":1,\"decision\":\"KEEP\"}]}")).isEqualTo(403);
        assertThat(estado(HttpMethod.POST, RUTA, clienteP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":1,\"decision\":\"KEEP\"}]}")).isEqualTo(403);
        assertThat(estado(HttpMethod.GET, RUTA, clienteP, p.slug(), null)).isEqualTo(403);
        assertThat(estado(HttpMethod.GET, RUTA + "/history", clienteP, p.slug(), null)).isEqualTo(403);
        assertThat(estado(HttpMethod.GET, RUTA, null, p.slug(), null)).isIn(401, 403);
    }

    @Test
    @Order(2)
    void laRevisionListaSoloLosAccesosPrivilegiadosDeSuEmpresa() throws Exception {
        JsonNode datos = leer(HttpMethod.GET, RUTA, adminP, p.slug(), null, 200);

        assertThat(datos.get("overdue").asBoolean()).isTrue();
        assertThat(datos.get("lastReview").isNull()).isTrue();
        assertThat(datos.get("items")).hasSize(1);
        JsonNode acceso = datos.get("items").get(0);
        assertThat(acceso.get("email").asText()).containsIgnoringCase("rvp");
        assertThat(acceso.get("reasons").get(0).asText()).isEqualTo("Administrador de la empresa");
        assertThat(datos.toString()).doesNotContainIgnoringCase("rvq");
    }

    @Test
    @Order(3)
    void unaRevisionIncompletaOSinMotivoSeRechazaSinGuardarNada() throws Exception {
        assertThat(estado(HttpMethod.POST, RUTA, adminP, p.slug(), "{\"decisions\":[]}")).isEqualTo(400);

        int accesoId = leer(HttpMethod.GET, RUTA, adminP, p.slug(), null, 200).get("items").get(0).get("assignmentId").asInt();
        assertThat(estado(HttpMethod.POST, RUTA, adminP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":999999,\"decision\":\"KEEP\"}]}")).isEqualTo(400);
        assertThat(estado(HttpMethod.POST, RUTA, adminP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":" + accesoId + ",\"decision\":\"REVOKE\"}]}")).isEqualTo(400);
        assertThat(estado(HttpMethod.POST, RUTA, adminP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":" + accesoId + ",\"decision\":\"REVOKE\",\"comment\":\"Ya no corresponde\"}]}"))
                .isEqualTo(400);

        assertThat(leer(HttpMethod.GET, RUTA + "/history", adminP, p.slug(), null, 200)).isEmpty();
    }

    @Test
    @Order(4)
    void mantenerTodosLosAccesosRegistraLaRevisionYLaDejaVigente() throws Exception {
        int accesoId = leer(HttpMethod.GET, RUTA, adminP, p.slug(), null, 200).get("items").get(0).get("assignmentId").asInt();

        JsonNode resumen = leer(HttpMethod.POST, RUTA, adminP, p.slug(),
                "{\"decisions\":[{\"assignmentId\":" + accesoId + ",\"decision\":\"KEEP\"}],\"notes\":\"Revisión inicial\"}", 200);
        assertThat(resumen.get("reviewedCount").asInt()).isEqualTo(1);
        assertThat(resumen.get("keptCount").asInt()).isEqualTo(1);
        assertThat(resumen.get("revokedCount").asInt()).isZero();
        assertThat(resumen.get("performedByEmail").asText()).containsIgnoringCase("rvp");

        JsonNode despues = leer(HttpMethod.GET, RUTA, adminP, p.slug(), null, 200);
        assertThat(despues.get("overdue").asBoolean()).isFalse();
        assertThat(despues.get("lastReview").get("notes").asText()).isEqualTo("Revisión inicial");
        assertThat(despues.get("nextDueAt").isNull()).isFalse();

        JsonNode historial = leer(HttpMethod.GET, RUTA + "/history", adminP, p.slug(), null, 200);
        assertThat(historial).hasSize(1);
        long revisionId = historial.get(0).get("id").asLong();
        JsonNode detalle = leer(HttpMethod.GET, RUTA + "/" + revisionId, adminP, p.slug(), null, 200);
        assertThat(detalle.get("entries")).hasSize(1);
        assertThat(detalle.get("entries").get(0).get("decision").asText()).isEqualTo("KEEP");
    }

    @Test
    @Order(5)
    void laEvidenciaDeUnaEmpresaNoSeVeDesdeOtra() throws Exception {
        long revisionDeP = leer(HttpMethod.GET, RUTA + "/history", adminP, p.slug(), null, 200).get(0).get("id").asLong();

        JsonNode enQ = leer(HttpMethod.GET, RUTA, adminQ, q.slug(), null, 200);
        assertThat(enQ.get("overdue").asBoolean()).isTrue();
        assertThat(enQ.get("lastReview").isNull()).isTrue();
        assertThat(enQ.toString()).doesNotContainIgnoringCase("rvp");
        assertThat(leer(HttpMethod.GET, RUTA + "/history", adminQ, q.slug(), null, 200)).isEmpty();
        assertThat(estado(HttpMethod.GET, RUTA + "/" + revisionDeP, adminQ, q.slug(), null)).isEqualTo(404);
        assertThat(estado(HttpMethod.GET, RUTA + "?companyId=" + p.companyId(), adminQ, q.slug(), null)).isEqualTo(200);
        assertThat(leer(HttpMethod.GET, RUTA + "?companyId=" + p.companyId(), adminQ, q.slug(), null, 200).toString())
                .doesNotContainIgnoringCase("rvp");
    }

    private JsonNode leer(HttpMethod verbo, String ruta, Cookie[] sesion, String slug, String cuerpo, int esperado) throws Exception {
        MvcResult resultado = mvc.perform(peticion(verbo, ruta, sesion, slug, cuerpo)).andReturn();
        assertThat(resultado.getResponse().getStatus())
                .as(verbo + " " + ruta + " -> " + resultado.getResponse().getContentAsString()).isEqualTo(esperado);
        return json.readTree(resultado.getResponse().getContentAsString()).get("data");
    }

    private int estado(HttpMethod verbo, String ruta, Cookie[] sesion, String slug, String cuerpo) throws Exception {
        return mvc.perform(peticion(verbo, ruta, sesion, slug, cuerpo)).andReturn().getResponse().getStatus();
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] sesion, String slug, String cuerpo) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta).header("Origin", "http://localhost:4200");
        if (slug != null) {
            peticion.header("X-Company-Slug", slug);
        }
        if (sesion != null) {
            peticion.cookie(sesion);
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return peticion;
    }

    private Cookie[] iniciarSesion(String slug, String usuario) throws Exception {
        MvcResult resultado = mvc.perform(peticion(HttpMethod.POST, "/auth/login", null, slug,
                "{\"slug\":\"" + slug + "\",\"username\":\"" + usuario + "\",\"password\":\""
                        + DosEmpresasSembradas.CLAVE + "\"}")).andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión de " + usuario + ": "
                + resultado.getResponse().getContentAsString()).isEqualTo(200);
        return resultado.getResponse().getCookies();
    }
}
