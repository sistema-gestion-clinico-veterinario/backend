package veterinaria.vargasvet.ers.seguridad;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import veterinaria.vargasvet.service.UsuarioService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-24. Con la aplicación completa y los flujos reales que guardan contraseñas (inicio de sesión que renueva el hash,
 * cambio de clave, alta de un cliente con clave temporal), comprueba que lo guardado es siempre un BCrypt del costo
 * operativo y que ninguna clave en texto plano queda en ninguna columna de texto de la base.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_claves;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class AlmacenamientoDeContrasenasIntegrationTest {

    private static final String BCRYPT_COSTO_12 = "^\\$2a\\$12\\$[./A-Za-z0-9]{53}$";
    private static final String CLAVE_NUEVA = "Nueva-Frase-Segura-2026";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private PasswordEncoder codificador;
    @Autowired private UsuarioService usuarioService;

    private DosEmpresasSembradas.Empresa clinica;

    @BeforeAll
    void sembrarClinica() {
        clinica = sembrador.sembrar("TKW");
    }

    @Test
    @Order(1)
    void unaClaveGuardadaConMenorCostoSeVuelveAGuardarConElOperativoAlIniciarSesion() throws Exception {
        String antiguo = new BCryptPasswordEncoder(10).encode(DosEmpresasSembradas.CLAVE);
        jdbc.update("UPDATE usuario_empresa_credencial SET password = ? WHERE usuario_id = "
                + "(SELECT id FROM usuario WHERE username = 'cliente2.tkw')", antiguo);
        assertThat(claveDe("cliente2.tkw")).startsWith("$2a$10$");

        assertThat(iniciarSesion("cliente2.tkw", DosEmpresasSembradas.CLAVE).getResponse().getStatus()).isEqualTo(200);

        assertThat(claveDe("cliente2.tkw")).matches(BCRYPT_COSTO_12);
        assertThat(iniciarSesion("cliente2.tkw", DosEmpresasSembradas.CLAVE).getResponse().getStatus())
                .as("la clave sigue siendo la misma después de renovar el hash").isEqualTo(200);
    }

    @Test
    @Order(2)
    void cambiarLaClaveGuardaUnHashDelCostoOperativoYLaAnteriorDejaDeServir() throws Exception {
        Cookie[] sesion = iniciarSesion("admin.tkw", DosEmpresasSembradas.CLAVE).getResponse().getCookies();

        MvcResult cambio = mvc.perform(peticion(HttpMethod.POST, "/auth/change-password", sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"" + DosEmpresasSembradas.CLAVE + "\",\"newPassword\":\"" + CLAVE_NUEVA + "\"}"))
                .andReturn();

        assertThat(cambio.getResponse().getStatus()).as(cambio.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(claveDe("admin.tkw")).matches(BCRYPT_COSTO_12);
        assertThat(iniciarSesion("admin.tkw", CLAVE_NUEVA).getResponse().getStatus()).isEqualTo(200);
        assertThat(iniciarSesion("admin.tkw", DosEmpresasSembradas.CLAVE).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @Order(3)
    void laClaveTemporalDeUnClienteNuevoSeGuardaCifrada() throws Exception {
        Cookie[] sesion = iniciarSesion("admin.tkw", CLAVE_NUEVA).getResponse().getCookies();
        jdbc.update("INSERT INTO aviso_privacidad (company_id, audiencia, version, contenido, contenido_hash, campos, vigente_desde, activo, creado_en) "
                + "VALUES (?, 'PROPIETARIOS_Y_AUTORIZADOS', 1, 'Aviso de prueba', 'hash-tkw', 'nombre,correo', CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP)",
                clinica.companyId());
        Integer rolCliente = jdbc.queryForObject("SELECT id FROM roles WHERE company_id = ? AND name = 'ROLE_CLIENTE'", Integer.class,
                clinica.companyId());

        MvcResult alta = mvc.perform(peticion(HttpMethod.POST, "/clients/guardians", sesion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"apellido\":\"Cliente\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"31000001\","
                                + "\"email\":\"nuevo.clave@tkw.test\",\"username\":\"nuevo.clave\",\"telefono\":\"922222222\","
                                + "\"direccion\":\"Calle Nueva 1\",\"roleIds\":[" + rolCliente + "],\"avisoInformado\":true,"
                                + "\"genero\":\"MASCULINO\",\"companyId\":" + clinica.companyId() + "}"))
                .andReturn();

        assertThat(alta.getResponse().getStatus()).as(alta.getResponse().getContentAsString()).isBetween(200, 201);
        assertThat(claveDe("nuevo.clave")).matches(BCRYPT_COSTO_12);
    }

    @Test
    @Order(4)
    void elHashSenueloDeLosUsuariosInexistentesTieneElMismoCostoQueUnaClaveReal() {
        Object servicio = AopProxyUtils.getSingletonTarget(usuarioService);
        String hash = ReflectionTestUtils.invokeMethod(servicio == null ? usuarioService : servicio, "hashSenuelo");

        assertThat(hash).matches(BCRYPT_COSTO_12);
        assertThat(codificador.upgradeEncoding(hash)).isFalse();
    }

    @Test
    @Order(5)
    void todaCredencialGuardadaEsUnBCryptDelCostoOperativo() {
        List<String> claves = jdbc.queryForList("SELECT password FROM usuario_empresa_credencial", String.class);

        assertThat(claves).hasSizeGreaterThanOrEqualTo(6).allSatisfy(clave -> assertThat(clave).matches(BCRYPT_COSTO_12));
    }

    @Test
    @Order(6)
    void ningunaColumnaDeTextoDeLaBaseGuardaUnaClaveEnTextoPlano() {
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT table_name, column_name FROM information_schema.columns WHERE UPPER(table_schema) = 'PUBLIC' "
                        + "AND UPPER(data_type) IN ('CHARACTER VARYING', 'CHARACTER LARGE OBJECT', 'CHARACTER')");
        List<String> encontradas = new ArrayList<>();

        for (Map<String, Object> columna : columnas) {
            String tabla = (String) columna.get("TABLE_NAME");
            String campo = (String) columna.get("COLUMN_NAME");
            for (String clave : List.of(DosEmpresasSembradas.CLAVE, CLAVE_NUEVA)) {
                Integer coincidencias = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tabla + "\" WHERE \"" + campo + "\" LIKE ?", Integer.class, "%" + clave + "%");
                if (coincidencias != null && coincidencias > 0) {
                    encontradas.add(tabla + "." + campo);
                }
            }
        }

        assertThat(columnas.size()).as("la prueba tiene dientes: revisó muchas columnas").isGreaterThan(200);
        assertThat(encontradas).as("Columnas con una contraseña en texto plano").isEmpty();
    }

    private String claveDe(String usuario) {
        return jdbc.queryForObject("SELECT c.password FROM usuario_empresa_credencial c JOIN usuario u ON u.id = c.usuario_id "
                + "WHERE u.username = ?", String.class, usuario);
    }

    private MvcResult iniciarSesion(String usuario, String clave) throws Exception {
        return mvc.perform(peticion(HttpMethod.POST, "/auth/login", null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + clinica.slug() + "\",\"username\":\"" + usuario + "\",\"password\":\"" + clave + "\"}"))
                .andReturn();
    }

    private MockHttpServletRequestBuilder peticion(HttpMethod verbo, String ruta, Cookie[] sesion) {
        MockHttpServletRequestBuilder peticion = request(verbo, ruta)
                .header("Origin", "http://localhost:4200")
                .header("X-Company-Slug", clinica.slug());
        return sesion == null ? peticion : peticion.cookie(sesion);
    }
}
