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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_perfil_ventanas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class PerfilYVentanasIntegrationTest {

    private static final List<String> CAMPOS_DE_VENTANA = List.of(
            "id", "codigo", "nombre", "grupo", "orden", "activo", "icono", "presentacionDefault", "vistas");
    private static final List<String> CAMPOS_DE_VISTA = List.of(
            "id", "codigo", "nombre", "ruta", "grupo", "orden", "ordenGrupo", "icono", "activo", "visibleMenu", "audience");

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private PasswordEncoder codificador;
    @Autowired private ObjectMapper json;

    private DosEmpresasSembradas.Empresa clinica;
    private Cookie[] administradorDeClinica;
    private Cookie[] plataforma;

    @BeforeAll
    void prepararEscenario() throws Exception {
        clinica = sembrador.sembrar("TKV");
        administradorDeClinica = iniciarSesion("/auth/login",
                "{\"slug\":\"" + clinica.slug() + "\",\"username\":\"admin.tkv\",\"password\":\"" + DosEmpresasSembradas.CLAVE + "\"}",
                clinica.slug());
        plataforma = abrirSesionDePlataforma();
    }

    @Test
    void elPerfilDeUnUsuarioDeLaClinicaDevuelveSusDatosYSusRoles() throws Exception {
        Integer personal = jdbc.queryForObject("SELECT id FROM usuario WHERE username = 'sinpermisos.tkv'", Integer.class);
        Integer rol = jdbc.queryForObject("SELECT rol_id FROM usuario_por_rol WHERE usuario_id = ?", Integer.class, personal);

        MvcResult respuesta = mvc.perform(peticion(HttpMethod.GET, "/auth/profile/" + personal,
                administradorDeClinica, clinica.slug())).andReturn();

        assertThat(respuesta.getResponse().getStatus()).as(cuerpo(respuesta)).isEqualTo(200);
        JsonNode perfil = json.readTree(cuerpo(respuesta)).path("data");
        assertThat(perfil.path("id").asInt()).isEqualTo(personal);
        assertThat(perfil.path("email").asText()).isEqualTo("sinpermisos.tkv@tkv.test");
        assertThat(perfil.path("nombre").asText()).isEqualTo("PersonalTKV");
        assertThat(perfil.path("systemRole").textValue()).isEqualTo("Sin permisos TKV");
        assertThat(perfil.path("roleIds").size()).isEqualTo(1);
        assertThat(perfil.path("roleIds").get(0).asInt()).isEqualTo(rol);
        assertThat(perfil.path("companyId").asInt()).isEqualTo(clinica.companyId());
    }

    @Test
    void elListadoDeModulosDevuelveCadaModuloConSusVistas() throws Exception {
        MvcResult respuesta = mvc.perform(peticion(HttpMethod.GET, "/admin/windows", plataforma, null)).andReturn();

        assertThat(respuesta.getResponse().getStatus()).as(cuerpo(respuesta)).isEqualTo(200);
        JsonNode modulos = json.readTree(cuerpo(respuesta)).path("data");
        assertThat(modulos.isArray()).isTrue();
        assertThat(modulos.size()).isGreaterThanOrEqualTo(1);
        int vistas = 0;
        for (JsonNode modulo : modulos) {
            assertThat(campos(modulo)).containsExactlyElementsOf(CAMPOS_DE_VENTANA);
            assertThat(modulo.path("activo").asBoolean()).isTrue();
            for (JsonNode vista : modulo.path("vistas")) {
                assertThat(campos(vista)).containsExactlyElementsOf(CAMPOS_DE_VISTA);
                vistas++;
            }
        }
        assertThat(vistas).as("al menos un módulo trae vistas").isGreaterThanOrEqualTo(1);
    }

    @Test
    void consultarUnModuloPorSuIdentificadorDevuelveElMismoFormato() throws Exception {
        Integer id = jdbc.queryForObject("SELECT MIN(ventana_id) FROM vistas WHERE ventana_id IS NOT NULL", Integer.class);

        MvcResult respuesta = mvc.perform(peticion(HttpMethod.GET, "/admin/windows/" + id, plataforma, null)).andReturn();

        assertThat(respuesta.getResponse().getStatus()).as(cuerpo(respuesta)).isEqualTo(200);
        JsonNode modulo = json.readTree(cuerpo(respuesta)).path("data");
        assertThat(campos(modulo)).containsExactlyElementsOf(CAMPOS_DE_VENTANA);
        assertThat(modulo.path("id").asInt()).isEqualTo(id);
        assertThat(modulo.path("vistas").size()).isGreaterThanOrEqualTo(1);
        for (JsonNode vista : modulo.path("vistas")) {
            assertThat(campos(vista)).containsExactlyElementsOf(CAMPOS_DE_VISTA);
        }
    }

    @Test
    void registrarUnModuloRespondeConElModuloCreado() throws Exception {
        MvcResult respuesta = mvc.perform(peticion(HttpMethod.POST, "/admin/windows", plataforma, null)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of(
                        "codigo", "modulo prueba", "nombre", "Módulo de prueba", "orden", 90, "activo", true)))).andReturn();

        assertThat(respuesta.getResponse().getStatus()).as(cuerpo(respuesta)).isEqualTo(201);
        JsonNode modulo = json.readTree(cuerpo(respuesta)).path("data");
        assertThat(campos(modulo)).containsExactlyElementsOf(CAMPOS_DE_VENTANA);
        assertThat(modulo.path("id").asInt()).isPositive();
        assertThat(modulo.path("codigo").asText()).isEqualTo("MODULO_PRUEBA");
        assertThat(modulo.path("nombre").asText()).isEqualTo("Módulo de prueba");
        assertThat(modulo.path("orden").asInt()).isEqualTo(90);
        assertThat(modulo.path("vistas").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ventanas WHERE codigo = 'MODULO_PRUEBA'", Integer.class)).isEqualTo(1);
    }

    @Test
    void actualizarUnModuloConLosMismosDatosRespondeConElModulo() throws Exception {
        Integer id = jdbc.queryForObject("SELECT MIN(ventana_id) FROM vistas WHERE ventana_id IS NOT NULL", Integer.class);
        String codigo = jdbc.queryForObject("SELECT codigo FROM ventanas WHERE id = ?", String.class, id);
        String nombre = jdbc.queryForObject("SELECT nombre FROM ventanas WHERE id = ?", String.class, id);
        Integer orden = jdbc.queryForObject("SELECT orden FROM ventanas WHERE id = ?", Integer.class, id);

        MvcResult respuesta = mvc.perform(peticion(HttpMethod.PUT, "/admin/windows/" + id, plataforma, null)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of(
                        "codigo", codigo, "nombre", nombre, "orden", orden, "activo", true)))).andReturn();

        assertThat(respuesta.getResponse().getStatus()).as(cuerpo(respuesta)).isEqualTo(200);
        JsonNode modulo = json.readTree(cuerpo(respuesta)).path("data");
        assertThat(campos(modulo)).containsExactlyElementsOf(CAMPOS_DE_VENTANA);
        assertThat(modulo.path("id").asInt()).isEqualTo(id);
        assertThat(modulo.path("codigo").asText()).isEqualTo(codigo);
        assertThat(modulo.path("nombre").asText()).isEqualTo(nombre);
        assertThat(modulo.path("vistas").size()).isGreaterThanOrEqualTo(1);
    }

    private List<String> campos(JsonNode nodo) {
        List<String> nombres = new ArrayList<>();
        nodo.fieldNames().forEachRemaining(nombres::add);
        return nombres;
    }

    private String cuerpo(MvcResult respuesta) throws Exception {
        return respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Cookie[] abrirSesionDePlataforma() throws Exception {
        Integer rol = jdbc.queryForObject("SELECT id FROM roles WHERE purpose = 'PLATFORM_ADMIN' ORDER BY id LIMIT 1", Integer.class);
        jdbc.update("INSERT INTO usuario (email, username, nombre, apellido, dni, activo, email_verified, company_id) "
                + "VALUES ('plataforma.v@softvet.test', 'plataforma.v', 'Plataforma', 'Soporte', '99999998', TRUE, TRUE, NULL)");
        Integer usuario = jdbc.queryForObject("SELECT id FROM usuario WHERE username = 'plataforma.v'", Integer.class);
        jdbc.update("INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, activada_con_google, created_at, updated_at) "
                + "VALUES (?, NULL, ?, TRUE, 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", usuario, codificador.encode(DosEmpresasSembradas.CLAVE));
        jdbc.update("INSERT INTO usuario_por_rol (usuario_id, rol_id, company_id, created_at) VALUES (?, ?, NULL, CURRENT_TIMESTAMP)", usuario, rol);
        return iniciarSesion("/auth/admin-login",
                "{\"username\":\"plataforma.v\",\"password\":\"" + DosEmpresasSembradas.CLAVE + "\"}", null);
    }

    private Cookie[] iniciarSesion(String ruta, String credenciales, String slug) throws Exception {
        MvcResult resultado = mvc.perform(peticion(HttpMethod.POST, ruta, null, slug)
                .contentType(MediaType.APPLICATION_JSON).content(credenciales)).andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión en " + ruta).isEqualTo(200);
        return resultado.getResponse().getCookies();
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] sesion, String slug) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta).header("Origin", "http://localhost:4200");
        if (slug != null) {
            peticion.header("X-Company-Slug", slug);
        }
        return sesion == null ? peticion : peticion.cookie(sesion);
    }
}
