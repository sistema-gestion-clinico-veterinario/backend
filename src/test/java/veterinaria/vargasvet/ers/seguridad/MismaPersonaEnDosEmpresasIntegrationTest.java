package veterinaria.vargasvet.ers.seguridad;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-14. Una misma persona con cuenta en dos clínicas (como cliente y como administradora) alterna entre ambas con
 * sesiones reales. Cada consulta, listado, reporte y exportación debe mostrar solo la clínica de la sesión, y no
 * cambiar por el hecho de que la persona también pertenezca a otra clínica.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_alternancia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class MismaPersonaEnDosEmpresasIntegrationTest {

    private static final String[] TABLAS_DE_LA_PERSONA = {
            "apoderado:user_id", "usuario_empresa_credencial:usuario_id", "usuario_membresia:usuario_id",
            "usuario_empresa_contacto:usuario_id", "usuario_por_rol:usuario_id"};

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DosEmpresasSembradas sembrador;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder codificador;

    private DosEmpresasSembradas.Empresa p;
    private DosEmpresasSembradas.Empresa q;
    private List<CatalogoEndpoints.Endpoint> listados;
    private Map<String, String> antesEnP;
    private Map<String, String> antesEnQ;
    private Cookie[] adminEnP;
    private Cookie[] adminEnQ;
    private Cookie[] clienteEnP;
    private Cookie[] clienteEnQ;

    @BeforeAll
    void prepararEscenario() throws Exception {
        p = sembrador.sembrar("TKP");
        q = sembrador.sembrar("TKQ");
        listados = CatalogoEndpoints.todos().stream()
                .filter(e -> e.verbo().equals("GET") && !e.tieneVariables() && !e.esPublico() && !e.ruta().startsWith("/auth/")
                        && !e.ruta().startsWith("/realtime") && !e.ruta().startsWith("/media") && !e.ruta().startsWith("/audit-logs"))
                .toList();
        sembrarPagos(p);
        sembrarPagos(q);

        Cookie[] soloP = iniciarSesion(p.slug(), "admin.tkp");
        Cookie[] soloQ = iniciarSesion(q.slug(), "admin.tkq");
        antesEnP = leerTodo(soloP, p);
        antesEnQ = leerTodo(soloQ, q);

        unir("admin.tkp", "admin.tkq");
        unir("cliente1.tkp", "cliente1.tkq");

        adminEnP = iniciarSesion(p.slug(), "admin.tkp");
        adminEnQ = iniciarSesion(q.slug(), "admin.tkp");
        clienteEnP = iniciarSesion(p.slug(), "cliente1.tkp");
        clienteEnQ = iniciarSesion(q.slug(), "cliente1.tkp");
    }

    @Test
    void unaClientaEnDosClinicasSoloVeLosDatosDeLaClinicaDeLaSesion() throws Exception {
        List<String> violaciones = new ArrayList<>();
        Map<String, String> enP = leerTodo(clienteEnP, p);
        Map<String, String> enQ = leerTodo(clienteEnQ, q);

        enP.forEach((ruta, cuerpo) -> { if (conDatosDe(cuerpo, "TKQ")) violaciones.add("en TKP, " + ruta + " muestra datos de TKQ"); });
        enQ.forEach((ruta, cuerpo) -> { if (conDatosDe(cuerpo, "TKP")) violaciones.add("en TKQ, " + ruta + " muestra datos de TKP"); });

        assertThat(violaciones).as("Datos de la otra clínica en las consultas de la clienta").isEmpty();
        assertThat(enP.values().stream().filter(c -> conDatosDe(c, "TKP")).count())
                .as("la prueba tiene dientes: en TKP sí ve sus datos").isGreaterThanOrEqualTo(3);
        assertThat(enQ.values().stream().filter(c -> conDatosDe(c, "TKQ")).count())
                .as("la prueba tiene dientes: en TKQ sí ve sus datos").isGreaterThanOrEqualTo(3);
    }

    @Test
    void unaAdministradoraEnDosClinicasSoloVeLosDatosDeLaClinicaDeLaSesion() throws Exception {
        List<String> violaciones = new ArrayList<>();
        Map<String, String> enP = leerTodo(adminEnP, p);
        Map<String, String> enQ = leerTodo(adminEnQ, q);

        enP.forEach((ruta, cuerpo) -> { if (conDatosDe(cuerpo, "TKQ")) violaciones.add("en TKP, " + ruta + " muestra datos de TKQ"); });
        enQ.forEach((ruta, cuerpo) -> { if (conDatosDe(cuerpo, "TKP")) violaciones.add("en TKQ, " + ruta + " muestra datos de TKP"); });

        assertThat(violaciones).as("Datos de la otra clínica en las consultas de la administradora").isEmpty();
        assertThat(enP.values().stream().filter(c -> conDatosDe(c, "TKP")).count()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void pertenecerAOtraClinicaNoCambiaNingunReporteNiListado() throws Exception {
        List<String> cambios = new ArrayList<>();
        Map<String, String> despuesEnP = leerTodo(adminEnP, p);
        Map<String, String> despuesEnQ = leerTodo(adminEnQ, q);

        comparar("TKP", antesEnP, despuesEnP, cambios);
        comparar("TKQ", antesEnQ, despuesEnQ, cambios);

        assertThat(cambios).as("Respuestas que cambian cuando la persona también es de otra clínica").isEmpty();
    }

    @Test
    void elAdministradorDePlataformaVeSoloLaEmpresaQueSeleccionaAlAlternar() throws Exception {
        Cookie[] plataforma = abrirSesionDePlataforma();
        List<String> violaciones = new ArrayList<>();

        Map<String, String> primeraEnP = leerTodo(plataforma, p, true);
        Map<String, String> enQ = leerTodo(plataforma, q, true);
        Map<String, String> segundaEnP = leerTodo(plataforma, p, true);

        primeraEnP.forEach((ruta, cuerpo) -> {
            if (conEmpresaElegida(ruta) && conDatosDe(cuerpo, "TKQ")) violaciones.add("seleccionada TKP, " + ruta + " muestra TKQ");
        });
        enQ.forEach((ruta, cuerpo) -> {
            if (conEmpresaElegida(ruta) && conDatosDe(cuerpo, "TKP")) violaciones.add("seleccionada TKQ, " + ruta + " muestra TKP");
        });
        List<String> arrastres = new ArrayList<>();
        comparar("TKP al volver", primeraEnP, segundaEnP, arrastres);

        assertThat(violaciones).as("Datos de la otra empresa al alternar").isEmpty();
        assertThat(arrastres).as("Respuestas de TKP que cambian tras visitar TKQ").isEmpty();
        assertThat(primeraEnP.entrySet().stream().filter(e -> conEmpresaElegida(e.getKey()) && conDatosDe(e.getValue(), "TKP")).count())
                .isGreaterThanOrEqualTo(8);
        assertThat(enQ.entrySet().stream().filter(e -> conEmpresaElegida(e.getKey()) && conDatosDe(e.getValue(), "TKQ")).count())
                .isGreaterThanOrEqualTo(8);
    }

    private boolean conEmpresaElegida(String ruta) {
        boolean directorioDePlataforma = ruta.startsWith("/admin/company/list") || ruta.startsWith("/clinical-reports/comparison")
                || ruta.startsWith("/dashboard/overview");
        return ruta.contains("companyId=") && !directorioDePlataforma;
    }

    @Test
    void desactivarUnaClinicaCortaElAccesoDeLaPersonaSoloAhiYNoEnLaOtra() throws Exception {
        jdbc.update("UPDATE company SET activo = FALSE WHERE id = ?", q.companyId());
        try {
            int enLaDesactivada = mvc.perform(peticion(HttpMethod.GET, "/pets", adminEnQ, q.slug())).andReturn().getResponse().getStatus();
            int enLaOtra = mvc.perform(peticion(HttpMethod.GET, "/pets", adminEnP, p.slug())).andReturn().getResponse().getStatus();

            assertThat(enLaDesactivada).as("clínica desactivada").isIn(401, 403);
            assertThat(enLaOtra).as("la otra clínica sigue funcionando").isEqualTo(200);
        } finally {
            jdbc.update("UPDATE company SET activo = TRUE WHERE id = ?", q.companyId());
        }
    }

    private Cookie[] abrirSesionDePlataforma() throws Exception {
        Integer rol = jdbc.queryForObject("SELECT id FROM roles WHERE purpose = 'PLATFORM_ADMIN' ORDER BY id LIMIT 1", Integer.class);
        jdbc.update("INSERT INTO usuario (email, username, nombre, apellido, dni, activo, email_verified, company_id) "
                + "VALUES ('plataforma@softvet.test', 'plataforma', 'Plataforma', 'Soporte', '99999999', TRUE, TRUE, NULL)");
        Integer usuario = jdbc.queryForObject("SELECT id FROM usuario WHERE username = 'plataforma'", Integer.class);
        jdbc.update("INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, activada_con_google, created_at, updated_at) "
                + "VALUES (?, NULL, ?, TRUE, 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", usuario, codificador.encode(DosEmpresasSembradas.CLAVE));
        jdbc.update("INSERT INTO usuario_por_rol (usuario_id, rol_id, company_id, created_at) VALUES (?, ?, NULL, CURRENT_TIMESTAMP)", usuario, rol);
        MvcResult resultado = mvc.perform(peticion(HttpMethod.POST, "/auth/admin-login", null, null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"plataforma\",\"password\":\"" + DosEmpresasSembradas.CLAVE + "\"}"))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión de plataforma: "
                + resultado.getResponse().getContentAsString()).isEqualTo(200);
        return resultado.getResponse().getCookies();
    }

    private void comparar(String clinica, Map<String, String> antes, Map<String, String> despues, List<String> cambios) {
        antes.forEach((ruta, cuerpoAntes) -> {
            String cuerpo = sinIdentidadDeLaPersona(cuerpoAntes);
            if (!cuerpo.equals(sinIdentidadDeLaPersona(despues.get(ruta))) && !esVolatil(ruta)) {
                String otro = sinIdentidadDeLaPersona(despues.get(ruta));
                int i = 0;
                while (i < cuerpo.length() && i < otro.length() && cuerpo.charAt(i) == otro.charAt(i)) i++;
                cambios.add(clinica + " " + ruta + " :: antes[" + cuerpo.substring(Math.max(0, i - 40), Math.min(cuerpo.length(), i + 90))
                        + "] despues[" + otro.substring(Math.max(0, i - 40), Math.min(otro.length(), i + 90)) + "]");
            }
        });
    }

    private String sinIdentidadDeLaPersona(String cuerpo) {
        return cuerpo == null ? "" : cuerpo
                .replaceAll("(Ana|Administrador)TK[PQ]", "$1")
                .replaceAll("ApellidoTK[PQ]", "Apellido")
                .replaceAll("(cliente1|admin)[.]tk[pq]@tk[pq][.]test", "$1@persona.test")
                .replaceAll("\"(userId|usuarioId)\":[0-9]+", "\"$1\":0")
                .replaceAll("\"lastActivity\":\"[^\"]*\"", "\"lastActivity\":\"-\"");
    }

    private boolean esVolatil(String ruta) {
        return ruta.startsWith("/auth") || ruta.startsWith("/profile") || ruta.startsWith("/menu") || ruta.startsWith("/navigation")
                || ruta.startsWith("/clients/portal/profile") || ruta.startsWith("/admin/audit-logs")
                || ruta.startsWith("/dashboard/overview");
    }

    private boolean conDatosDe(String cuerpo, String marca) {
        List<String> conEspacio = List.of("Clinica ", "Consulta ", "Producto ", "Motivo confidencial ", "Categoria ",
                "Marca ", "Diagnostico ", "Rol personalizado ", "Unidad ");
        List<String> pegados = List.of("Luna", "Nube", "Veterinario", "Beatriz");
        return conEspacio.stream().anyMatch(dato -> cuerpo.contains(dato + marca))
                || pegados.stream().anyMatch(dato -> cuerpo.contains(dato + marca))
                || cuerpo.contains("HC-" + marca + "-");
    }

    private Map<String, String> leerTodo(Cookie[] sesion, DosEmpresasSembradas.Empresa clinica) throws Exception {
        return leerTodo(sesion, clinica, false);
    }

    private Map<String, String> leerTodo(Cookie[] sesion, DosEmpresasSembradas.Empresa clinica, boolean comoPlataforma)
            throws Exception {
        Map<String, String> respuestas = new LinkedHashMap<>();
        String hoy = LocalDate.now().toString();
        String rango = "fechaDesde=2020-01-01&fechaHasta=" + hoy + "&desde=2020-01-01&hasta=" + hoy + "&startDate=2020-01-01T00:00:00";
        for (CatalogoEndpoints.Endpoint endpoint : listados) {
            for (String consulta : List.of("", "?companyId=" + clinica.companyId(), "?" + rango + "&companyId=" + clinica.companyId())) {
                MockHttpServletRequestBuilder lectura = comoPlataforma
                        ? peticion(HttpMethod.GET, endpoint.ruta() + consulta, sesion, null)
                                .header("X-Company-Id", String.valueOf(clinica.companyId()))
                        : peticion(HttpMethod.GET, endpoint.ruta() + consulta, sesion, clinica.slug());
                MvcResult resultado = mvc.perform(lectura).andReturn();
                respuestas.put(endpoint.ruta() + consulta, resultado.getResponse().getStatus() + " " + resultado.getResponse().getContentAsString());
            }
        }
        return respuestas;
    }

    private void sembrarPagos(DosEmpresasSembradas.Empresa clinica) {
        Integer usuario = jdbc.queryForObject("SELECT a.user_id FROM apoderado a WHERE a.id = ?", Integer.class, clinica.apoderado1Id());
        jdbc.update("INSERT INTO purchases (total, created_at, payment_status, user_id, cita_id, tipo_purchase, metodo_pago) "
                + "VALUES (100.00, CURRENT_TIMESTAMP, 'PAID', ?, ?, 'SERVICIO_CITA', 'EFECTIVO')", usuario, clinica.citaId());
    }

    private void unir(String usuarioQueSeQueda, String usuarioQueSeFusiona) {
        Integer destino = jdbc.queryForObject("SELECT id FROM usuario WHERE username = ?", Integer.class, usuarioQueSeQueda);
        Integer origen = jdbc.queryForObject("SELECT id FROM usuario WHERE username = ?", Integer.class, usuarioQueSeFusiona);
        for (String tabla : TABLAS_DE_LA_PERSONA) {
            String[] partes = tabla.split(":");
            jdbc.update("UPDATE " + partes[0] + " SET " + partes[1] + " = ? WHERE " + partes[1] + " = ?", destino, origen);
        }
        jdbc.update("UPDATE usuario SET company_id = NULL WHERE id = ?", destino);
    }

    private Cookie[] iniciarSesion(String slug, String usuario) throws Exception {
        MvcResult resultado = mvc.perform(peticion(HttpMethod.POST, "/auth/login", null, slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"username\":\"" + usuario + "\",\"password\":\""
                                + DosEmpresasSembradas.CLAVE + "\"}"))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión de " + usuario + " en " + slug
                + ": " + resultado.getResponse().getContentAsString()).isEqualTo(200);
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
