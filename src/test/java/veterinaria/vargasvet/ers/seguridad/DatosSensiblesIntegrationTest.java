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
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.enums.DataScope;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-28. Con sesiones reales, quien solo puede consultar clientes y personal recibe el documento, el teléfono y el
 * correo parcialmente ocultos, y quien administra esos registros los recibe completos.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_datos_sensibles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class DatosSensiblesIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private UsuarioPorRolRepository asignaciones;
    @Autowired private VistaRepository vistas;
    @Autowired private RolVistaPermisoRepository permisos;

    private DosEmpresasSembradas.Empresa clinica;
    private Cookie[] administrador;
    private Cookie[] soloConsulta;

    @BeforeAll
    void prepararEscenario() throws Exception {
        clinica = sembrador.sembrar("DSA");
        Role rolDeConsulta = asignaciones.findActiveAssignmentsByCompanyIdAndRoleScope(clinica.companyId(), RoleScope.STAFF)
                .stream()
                .filter(asignacion -> "sinpermisos.dsa@dsa.test".equals(asignacion.getUsuario().getEmail()))
                .findFirst().orElseThrow().getRol();
        conceder(rolDeConsulta, "VISTA_CLIENTES");
        conceder(rolDeConsulta, "VISTA_EMPLEADOS");
        administrador = iniciarSesion("admin.dsa");
        soloConsulta = iniciarSesion("sinpermisos.dsa");
    }

    @Test
    void elAdministradorDeClientesVeDocumentoTelefonoYCorreoCompletos() throws Exception {
        JsonNode cliente = clientePorCorreo(administrador, "cliente1.dsa@dsa.test");

        assertThat(cliente.get("email").asText()).isEqualTo("cliente1.dsa@dsa.test");
        assertThat(cliente.get("numeroDocumento").asText()).matches("\\d{8}");
        assertThat(cliente.get("telefono").asText()).matches("\\d{9}");
    }

    @Test
    void quienSoloConsultaClientesRecibeLosDatosParcialmenteOcultos() throws Exception {
        JsonNode completo = clientePorCorreo(administrador, "cliente1.dsa@dsa.test");
        JsonNode parcial = clientePorCorreo(soloConsulta, "c***@dsa.test");

        assertThat(parcial.get("email").asText()).isEqualTo("c***@dsa.test");
        assertThat(parcial.get("numeroDocumento").asText()).isEqualTo("****" + ultimos(completo, "numeroDocumento", 4));
        assertThat(parcial.get("telefono").asText()).isEqualTo("******" + ultimos(completo, "telefono", 3));
        assertThat(parcial.get("nombre").asText()).isEqualTo(completo.get("nombre").asText());
    }

    @Test
    void elDetalleParaEditarTambienSeOcultaAQuienSoloConsulta() throws Exception {
        String ruta = "/clients/guardians/" + clinica.apoderado1Id();
        JsonNode completo = datos(administrador, ruta);
        JsonNode parcial = datos(soloConsulta, ruta);

        assertThat(completo.get("email").asText()).isEqualTo("cliente1.dsa@dsa.test");
        assertThat(parcial.get("email").asText()).isEqualTo("c***@dsa.test");
        assertThat(parcial.get("numeroDocumento").asText()).startsWith("****").doesNotContain(completo.get("numeroDocumento").asText());
        assertThat(parcial.get("telefono").asText()).startsWith("******");
    }

    @Test
    void elPersonalSeProtegeIgualConSuPropiaVista() throws Exception {
        JsonNode completo = empleadoPorCorreo(administrador, "vet.dsa@dsa.test");
        JsonNode parcial = empleadoPorCorreo(soloConsulta, "v***@dsa.test");

        assertThat(completo.get("telefono").asText()).matches("\\d{9}");
        assertThat(parcial.get("telefono").asText()).startsWith("******");
        assertThat(parcial.get("email").asText()).isEqualTo("v***@dsa.test");
    }

    @Test
    void ningunaRespuestaParcialContieneElValorCompleto() throws Exception {
        JsonNode completo = clientePorCorreo(administrador, "cliente1.dsa@dsa.test");
        String listado = respuesta(soloConsulta, "/clients/guardians?size=50");

        assertThat(listado).doesNotContain(completo.get("email").asText());
        assertThat(listado).doesNotContain(completo.get("numeroDocumento").asText());
        assertThat(listado).doesNotContain(completo.get("telefono").asText());
    }

    private JsonNode clientePorCorreo(Cookie[] sesion, String correo) throws Exception {
        return buscar(datos(sesion, "/clients/guardians?size=50").get("content"), correo);
    }

    private JsonNode empleadoPorCorreo(Cookie[] sesion, String correo) throws Exception {
        return buscar(datos(sesion, "/admin/employees?size=50").get("content"), correo);
    }

    private JsonNode buscar(JsonNode lista, String correo) {
        for (JsonNode fila : lista) {
            if (correo.equals(fila.get("email").asText())) {
                return fila;
            }
        }
        throw new AssertionError("No apareció " + correo + " en " + lista);
    }

    private String ultimos(JsonNode fila, String campo, int cantidad) {
        String valor = fila.get(campo).asText();
        return valor.substring(valor.length() - cantidad);
    }

    private JsonNode datos(Cookie[] sesion, String ruta) throws Exception {
        return json.readTree(respuesta(sesion, ruta)).get("data");
    }

    private String respuesta(Cookie[] sesion, String ruta) throws Exception {
        MvcResult resultado = mvc.perform(peticion(HttpMethod.GET, ruta, sesion, null)).andReturn();
        assertThat(resultado.getResponse().getStatus()).as(ruta + " -> " + resultado.getResponse().getContentAsString())
                .isEqualTo(200);
        return resultado.getResponse().getContentAsString();
    }

    private void conceder(Role rol, String codigoVista) {
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setRol(rol);
        permiso.setVista(vistas.findByCodigo(codigoVista).orElseThrow());
        permiso.setLeer(true);
        permiso.setDataScope(DataScope.COMPANY);
        permisos.save(permiso);
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] sesion, String cuerpo) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta).header("Origin", "http://localhost:4200")
                .header("X-Company-Slug", clinica.slug());
        if (sesion != null) {
            peticion.cookie(sesion);
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return peticion;
    }

    private Cookie[] iniciarSesion(String usuario) throws Exception {
        MvcResult resultado = mvc.perform(peticion(HttpMethod.POST, "/auth/login", null,
                "{\"slug\":\"" + clinica.slug() + "\",\"username\":\"" + usuario + "\",\"password\":\""
                        + DosEmpresasSembradas.CLAVE + "\"}")).andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión de " + usuario + ": "
                + resultado.getResponse().getContentAsString()).isEqualTo(200);
        return resultado.getResponse().getCookies();
    }
}
