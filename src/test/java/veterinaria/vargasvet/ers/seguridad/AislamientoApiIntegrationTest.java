package veterinaria.vargasvet.ers.seguridad;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * RNF-02. Con dos clínicas con datos reales de cada tipo, intenta leer, crear, editar y eliminar lo de la otra clínica
 * (y lo de otro cliente) por la API real, con la seguridad completa y sesiones reales. Pasa solo si:
 * ninguna respuesta contiene datos de la otra parte, ninguna fila ajena cambia y nada se permite sin permiso.
 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(ConfiguracionDePruebaSeguridad.class)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"})
class AislamientoApiIntegrationTest {

    private static final int MAX_ID = 8;
    private static final Pattern REGLA_DE_CLIENTE = Pattern.compile("VISTA_MIS_|VISTA_MI_HISTORIAL|VISTA_APODERADO|VISTA_PROFILE|isAuthenticated\\(\\)");

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private CompanyRepository companias;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ApoderadoRepository apoderados;
    @Autowired private MascotaRepository mascotas;
    @Autowired private EmpleadoRepository empleados;
    @Autowired private ServiciosVeterinariosRepository servicios;
    @Autowired private CitaRepository citas;
    @Autowired private HistoriaClinicaRepository historias;
    @Autowired private ConsultaRepository consultas;
    @Autowired private DiagnosticoRepository diagnosticos;
    @Autowired private TratamientoRepository tratamientos;
    @Autowired private PrescripcionRepository prescripciones;
    @Autowired private CategoriaProductoRepository categorias;
    @Autowired private MarcaProductoRepository marcas;
    @Autowired private UnidadMedidaRepository unidades;
    @Autowired private ProductoRepository productos;
    @Autowired private LoteRepository lotes;
    @Autowired private RoleRepository roles;
    @Autowired private UsuarioPorRolRepository asignaciones;
    @Autowired private UsuarioEmpresaContactoRepository contactos;
    @Autowired private UsuarioEmpresaCredencialRepository credenciales;
    @Autowired private UsuarioMembresiaRepository membresias;
    @Autowired private VistaRepository vistas;
    @Autowired private RolVistaPermisoRepository permisos;
    @Autowired private CompanyRoleProvisioningService aprovisionamiento;
    @Autowired private DosEmpresasSembradas sembrador;

    private DosEmpresasSembradas.Empresa a;
    private DosEmpresasSembradas.Empresa b;
    private Sesion adminA, cliente1A, cliente2A, sinPermisosA, adminB, clienteB, sinPermisosB;
    private List<CatalogoEndpoints.Endpoint> endpoints;

    record Respuesta(int estado, String cuerpo) {
        boolean exito() { return estado >= 200 && estado < 300; }
    }

    record Sesion(String nombre, String slug, Cookie[] cookies) {}

    @BeforeAll
    void prepararEscenario() throws Exception {
        a = sembrador.sembrar("TKA");
        b = sembrador.sembrar("TKB");
        endpoints = CatalogoEndpoints.todos();
        adminA = iniciarSesion("adminA", a.slug(), "admin.tka");
        cliente1A = iniciarSesion("cliente1A", a.slug(), "cliente1.tka");
        cliente2A = iniciarSesion("cliente2A", a.slug(), "cliente2.tka");
        sinPermisosA = iniciarSesion("sinPermisosA", a.slug(), "sinpermisos.tka");
        adminB = iniciarSesion("adminB", b.slug(), "admin.tkb");
        clienteB = iniciarSesion("clienteB", b.slug(), "cliente1.tkb");
        sinPermisosB = iniciarSesion("sinPermisosB", b.slug(), "sinpermisos.tkb");
    }

    // ------------------------------------------------------------------ 1. lectura por identificador

    @Test
    @Order(1)
    void leerPorIdentificadorNuncaEntregaDatosDeOtraEmpresaNiDeOtroCliente() {
        List<String> violaciones = new ArrayList<>();
        List<CatalogoEndpoints.Endpoint> lecturas = lecturasPorId();
        assertThat(lecturas).hasSizeGreaterThan(30);

        for (Sesion intruso : List.of(adminB, clienteB, sinPermisosB)) {
            violaciones.addAll(buscarFugas(intruso, lecturas, a, List.of("TKA")));
        }
        for (Sesion intruso : List.of(adminA, cliente1A, cliente2A, sinPermisosA)) {
            violaciones.addAll(buscarFugas(intruso, lecturas, b, List.of("TKB")));
        }
        violaciones.addAll(buscarFugas(cliente2A, lecturas, a, privadosDelCliente1()));
        violaciones.addAll(buscarFugas(cliente1A, lecturas, a, privadosDelCliente2()));

        assertThat(violaciones).as("Respuestas con datos que no le corresponden").isEmpty();
    }

    @Test
    @Order(2)
    void laPruebaTieneDientesLosDuenosSiVenSusDatosPorLosMismosEndpoints() {
        List<CatalogoEndpoints.Endpoint> lecturas = lecturasPorId();

        int aciertosAdmin = contarAciertos(adminA, lecturas, a, "TKA");
        int aciertosCliente = contarAciertos(cliente1A, lecturas, a, "LunaTKA");
        int aciertosAdminB = contarAciertos(adminB, lecturas, b, "TKB");

        assertThat(aciertosAdmin).as("lecturas por id que la clínica A sí puede ver").isGreaterThanOrEqualTo(12);
        assertThat(aciertosCliente).as("lecturas por id que el cliente 1 sí puede ver").isGreaterThanOrEqualTo(1);
        assertThat(aciertosAdminB).as("lecturas por id que la clínica B sí puede ver").isGreaterThanOrEqualTo(12);
    }

    // ------------------------------------------------------------------ 2. listados y parámetros de empresa

    @Test
    @Order(3)
    void losListadosYElParametroDeEmpresaNoMezclanDatosDeOtraClinica() {
        List<CatalogoEndpoints.Endpoint> listados = endpoints.stream()
                .filter(e -> e.verbo().equals("GET") && !e.tieneVariables() && !e.esPublico()).toList();
        List<String> violaciones = new ArrayList<>();
        Map<Sesion, DosEmpresasSembradas.Empresa> ajena = new LinkedHashMap<>();
        for (Sesion s : List.of(adminB, clienteB, sinPermisosB)) ajena.put(s, a);
        for (Sesion s : List.of(adminA, cliente1A, cliente2A, sinPermisosA)) ajena.put(s, b);
        for (var entrada : ajena.entrySet()) {
            String prohibida = "TK" + entrada.getValue().marca().substring(2);
            for (CatalogoEndpoints.Endpoint endpoint : listados) {
                for (String consulta : List.of("", "?companyId=" + entrada.getValue().companyId(),
                        "?empresaId=" + entrada.getValue().companyId())) {
                    Respuesta respuesta = llamar(entrada.getKey(), "GET", endpoint.ruta() + consulta, null);
                    if (respuesta.cuerpo().contains(prohibida)) {
                        violaciones.add(entrada.getKey().nombre() + " " + endpoint.ruta() + consulta + " -> " + respuesta.estado());
                    }
                }
            }
        }
        for (CatalogoEndpoints.Endpoint endpoint : listados) {
            Respuesta r2 = llamar(cliente2A, "GET", endpoint.ruta(), null);
            for (String privado : privadosDelCliente1()) {
                if (r2.cuerpo().contains(privado)) violaciones.add("cliente2A " + endpoint.ruta() + " muestra " + privado);
            }
            Respuesta r1 = llamar(cliente1A, "GET", endpoint.ruta(), null);
            for (String privado : privadosDelCliente2()) {
                if (r1.cuerpo().contains(privado)) violaciones.add("cliente1A " + endpoint.ruta() + " muestra " + privado);
            }
        }

        assertThat(violaciones).as("Listados con datos que no le corresponden").isEmpty();
        assertThat(contarAciertosListados(adminA, listados, "TKA")).as("listados que A sí puede ver").isGreaterThanOrEqualTo(8);
    }

    // ------------------------------------------------------------------ 3. crear, editar y eliminar lo ajeno

    @Test
    @Order(40)
    void escribirEnLoAjenoNoCambiaNingunaFilaDeLaOtraClinicaNiDeOtroCliente() {
        List<CatalogoEndpoints.Endpoint> escrituras = endpoints.stream()
                .filter(e -> !e.verbo().equals("GET") && !e.esPublico() && !e.esAutoservicio() && !e.ruta().startsWith("/auth/")
                        && !e.ruta().startsWith("/realtime") && !e.ruta().startsWith("/media")).toList();
        assertThat(escrituras).hasSizeGreaterThan(100);
        List<String> violaciones = new ArrayList<>();

        atacarYComparar(List.of(adminB, clienteB, sinPermisosB), escrituras, a, a.filas(), violaciones);
        atacarYComparar(List.of(adminA, cliente1A, cliente2A, sinPermisosA), escrituras, b, b.filas(), violaciones);
        atacarYComparar(List.of(cliente2A), escrituras, a, filasDe(a, "cliente1"), violaciones);
        atacarYComparar(List.of(cliente1A), escrituras, a, filasDe(a, "cliente2"), violaciones);
        atacarYComparar(List.of(sinPermisosA), escrituras, a, filasDe(a, "comun", "cliente1", "cliente2"), violaciones);

        assertThat(violaciones).as("Operaciones que modificaron filas ajenas").isEmpty();
    }

    // ------------------------------------------------------------------ 4. sin permiso, todos los endpoints

    @Test
    @Order(50)
    void sinPermisoNingunEndpointRespondeConExitoSalvoLosPropiosDeLaCuenta() {
        List<String> violaciones = new ArrayList<>();
        for (CatalogoEndpoints.Endpoint endpoint : endpoints) {
            if (endpoint.esPublico() || endpoint.ruta().startsWith("/auth/")) continue;
            for (String url : urls(endpoint, a, 1)) {
                Respuesta anonima = llamar(null, endpoint.verbo(), url, cuerpoPara(endpoint));
                if (anonima.estado() != 401) {
                    violaciones.add("anónimo " + endpoint.clave() + " no fue rechazado con 401: " + anonima.estado());
                }
            }
        }
        Predicate<CatalogoEndpoints.Endpoint> soloSesion =
                e -> e.regla() == null ? e.esAutoservicio() : e.regla().replace(" ", "").equals("isAuthenticated()");
        Predicate<CatalogoEndpoints.Endpoint> portal =
                e -> e.regla() == null ? e.esAutoservicio() : REGLA_DE_CLIENTE.matcher(e.regla()).find();
        verificarSinPermiso(sinPermisosA, a, soloSesion, violaciones);
        verificarSinPermiso(sinPermisosB, b, soloSesion, violaciones);
        verificarSinPermiso(cliente1A, a, portal, violaciones);
        verificarSinPermiso(clienteB, b, portal, violaciones);

        assertThat(violaciones).as("Respuestas con éxito sin permiso").isEmpty();
    }

    // ------------------------------------------------------------------ 5. crear y editar con datos válidos en lo ajeno

    /** Una operación de escritura con datos válidos: el dueño la hace con éxito (control) y el intruso no debe lograrla. */
    record Intento(String nombre, String verbo, String ruta, String cuerpoControl, String cuerpoIntruso, String tabla, Long idFila,
                   Sesion dueno, List<Sesion> intrusos) {}

    private List<Intento> intentosValidos(DosEmpresasSembradas.Empresa e, Sesion dueno, List<Sesion> intrusosStaff,
                                          Sesion duenoCliente, List<Sesion> intrusosCliente) {
        String dni = jdbc.queryForObject("SELECT numero_documento FROM apoderado WHERE id = ?", String.class, e.apoderado1Id());
        String correo = jdbc.queryForObject("SELECT u.email FROM usuario u JOIN apoderado a ON a.user_id = u.id WHERE a.id = ?",
                String.class, e.apoderado1Id());
        String m = e.marca();
        String futuro = java.time.LocalDateTime.now().plusDays(9).withHour(10).withMinute(0).withSecond(0).withNano(0).toString();
        String futuro2 = java.time.LocalDateTime.now().plusDays(11).withHour(11).withMinute(0).withSecond(0).withNano(0).toString();
        Long raza = razaDe(e);
        darTurno(e, java.time.LocalDateTime.now().plusDays(9));
        darTurno(e, java.time.LocalDateTime.now().plusDays(11));
        Long marcaId = marcaDe(e);
        Integer rolCliente = jdbc.queryForObject("SELECT id FROM roles WHERE company_id = ? AND name = 'ROLE_CLIENTE'", Integer.class, e.companyId());
        List<Intento> intentos = new ArrayList<>();

        intentos.add(new Intento("crear mascota para un cliente ajeno", "POST", "/pets",
                "{\"nombreCompleto\":\"Ctl" + m + "\",\"especie\":\"PERRO\",\"razaId\":" + raza + ",\"sexo\":\"MACHO\",\"fechaNacimiento\":\"2020-01-01\",\"apoderadoId\":" + e.apoderado1Id() + "}",
                "{\"nombreCompleto\":\"Intruso\",\"especie\":\"PERRO\",\"razaId\":" + raza + ",\"sexo\":\"MACHO\",\"fechaNacimiento\":\"2020-01-01\",\"apoderadoId\":" + e.apoderado1Id() + "}",
                "mascota", null, dueno, intrusosStaff));
        intentos.add(new Intento("editar la mascota ajena", "PUT", "/pets/" + e.mascota1Id(),
                "{\"nombreCompleto\":\"Luna" + m + "\",\"especie\":\"PERRO\",\"razaId\":" + raza + ",\"sexo\":\"MACHO\",\"fechaNacimiento\":\"2020-01-01\",\"color\":\"Control\",\"apoderadoId\":" + e.apoderado1Id() + "}",
                "{\"nombreCompleto\":\"Intruso\",\"especie\":\"PERRO\",\"razaId\":" + raza + ",\"sexo\":\"HEMBRA\",\"fechaNacimiento\":\"2020-01-01\",\"color\":\"Intruso\",\"apoderadoId\":" + e.apoderado1Id() + "}",
                "mascota", e.mascota1Id(), dueno, intrusosStaff));
        intentos.add(new Intento("editar al cliente ajeno", "PUT", "/clients/guardians/" + e.apoderado1Id(),
                "{\"nombre\":\"Ana" + m + "\",\"apellido\":\"Apellido" + m + "\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dni
                        + "\",\"email\":\"" + correo + "\",\"telefono\":\"900000001\",\"direccion\":\"Calle Control 123\",\"genero\":\"FEMENINO\",\"observaciones\":\"Control\"}",
                "{\"nombre\":\"Intruso\",\"apellido\":\"Intruso\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dni
                        + "\",\"email\":\"" + correo + "\",\"telefono\":\"911111111\",\"direccion\":\"Calle Intruso 666\",\"genero\":\"FEMENINO\",\"observaciones\":\"Intruso\"}",
                "apoderado", e.apoderado1Id(), dueno, intrusosStaff));
        intentos.add(new Intento("registrar un cliente en la otra clínica", "POST", "/clients/guardians",
                "{\"nombre\":\"Nuevo\",\"apellido\":\"Control" + m + "\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"3" + digitosDe(m)
                        + "\",\"email\":\"nuevo.ctl@" + m.toLowerCase() + ".test\",\"username\":\"nuevo.ctl." + m.toLowerCase()
                        + "\",\"telefono\":\"922222222\",\"direccion\":\"Calle Nueva 1\",\"avisoInformado\":true,\"roleIds\":[" + rolCliente + "],\"genero\":\"MASCULINO\",\"companyId\":" + e.companyId() + "}",
                "{\"nombre\":\"Intruso\",\"apellido\":\"Intruso\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"4" + digitosDe(m)
                        + "\",\"email\":\"intruso@" + m.toLowerCase() + ".test\",\"username\":\"intruso." + m.toLowerCase()
                        + "\",\"telefono\":\"933333333\",\"direccion\":\"Calle Intruso 2\",\"avisoInformado\":true,\"roleIds\":[" + rolCliente + "],\"genero\":\"MASCULINO\",\"companyId\":" + e.companyId() + "}",
                "apoderado", null, dueno, intrusosStaff));
        intentos.add(new Intento("editar la consulta clínica ajena", "PUT", "/consultations/" + e.consultaId(),
                "{\"version\":0,\"tipoConsulta\":\"CONTROL_RUTINA\",\"vacunacionAplicada\":false,\"desparasitacionAplicada\":false,\"motivoConsulta\":\"Motivo confidencial " + m + "\",\"anamnesis\":\"Control\"}",
                "{\"version\":1,\"tipoConsulta\":\"EMERGENCIA\",\"vacunacionAplicada\":true,\"desparasitacionAplicada\":true,\"motivoConsulta\":\"Intruso\",\"anamnesis\":\"Intruso\"}",
                "consulta", e.consultaId(), dueno, intrusosStaff));
        intentos.add(new Intento("agregar una receta a la consulta ajena", "POST", "/prescriptions/consultation/" + e.consultaId(),
                "{\"medicamento\":\"Ctl" + m + "\",\"dosis\":\"1 tableta\",\"frecuencia\":\"Cada 8 horas\",\"duracionDias\":5,\"viaAdministracion\":\"Oral\",\"instrucciones\":\"Control\",\"fechaInicio\":\"" + java.time.LocalDate.now() + "\"}",
                "{\"medicamento\":\"Intruso\",\"dosis\":\"9 tabletas\",\"frecuencia\":\"Cada hora\",\"duracionDias\":9,\"viaAdministracion\":\"Oral\",\"instrucciones\":\"Intruso\",\"fechaInicio\":\"" + java.time.LocalDate.now() + "\"}",
                "prescripciones", null, dueno, intrusosStaff));
        intentos.add(new Intento("agregar un diagnóstico a la consulta ajena", "POST", "/diagnoses/consultation/" + e.consultaId(),
                "{\"nombre\":\"Ctl" + m + "\",\"tipo\":\"DEFINITIVO\",\"estado\":\"ACTIVO\"}",
                "{\"nombre\":\"Intruso\",\"tipo\":\"DEFINITIVO\",\"estado\":\"ACTIVO\"}",
                "diagnosticos", null, dueno, intrusosStaff));
        intentos.add(new Intento("agregar un tratamiento a la consulta ajena", "POST", "/treatments/consultation/" + e.consultaId(),
                "{\"nombre\":\"Ctl" + m + "\",\"estado\":\"ACTIVO\",\"fechaInicio\":\"" + java.time.LocalDate.now() + "\"}",
                "{\"nombre\":\"Intruso\",\"estado\":\"ACTIVO\",\"fechaInicio\":\"" + java.time.LocalDate.now() + "\"}",
                "tratamientos", null, dueno, intrusosStaff));
        intentos.add(new Intento("editar el producto ajeno", "PUT", "/productos/sku/" + e.sku(),
                "{\"nombre\":\"Producto " + m + "\",\"categoriaId\":" + e.categoriaId() + ",\"marcaId\":" + marcaId + ",\"precio\":25.00,\"stock\":10,\"controlStock\":\"LOTES\",\"stockMinimo\":2,\"requiereReceta\":false,\"aplicacionEspecie\":\"USO_GENERAL\",\"descripcion\":\"Control\"}",
                "{\"nombre\":\"Intruso\",\"categoriaId\":" + e.categoriaId() + ",\"marcaId\":" + marcaId + ",\"precio\":26.00,\"stock\":0,\"controlStock\":\"LOTES\",\"stockMinimo\":0,\"requiereReceta\":false,\"aplicacionEspecie\":\"USO_GENERAL\",\"descripcion\":\"Intruso\"}",
                "producto", e.productoId(), dueno, intrusosStaff));
        intentos.add(new Intento("agregar un lote al producto ajeno", "POST", "/lotes",
                "{\"productoId\":" + e.productoId() + ",\"numeroLote\":\"CTL-" + m + "\",\"fechaVencimiento\":\"2027-12-01\",\"cantidad\":5}",
                "{\"productoId\":" + e.productoId() + ",\"numeroLote\":\"INTRUSO\",\"fechaVencimiento\":\"2027-12-01\",\"cantidad\":999}",
                "lote", null, dueno, intrusosStaff));
        intentos.add(new Intento("editar el servicio ajeno", "PUT", "/services/" + e.servicioId(),
                "{\"nombre\":\"Consulta " + m + "\",\"descripcion\":\"Control\",\"precio\":100.00,\"duracionEstimada\":30}",
                "{\"nombre\":\"Intruso\",\"descripcion\":\"Intruso\",\"precio\":101.00,\"duracionEstimada\":30}",
                "servicios", e.servicioId(), dueno, intrusosStaff));
        intentos.add(new Intento("editar la empresa ajena", "PUT", "/admin/company/" + e.companyId(),
                "{\"name\":\"Clinica " + m + "\",\"phone\":\"999888777\",\"email\":\"contacto@" + m.toLowerCase() + ".test\",\"ruc\":\"" + jdbc.queryForObject("SELECT ruc FROM company WHERE id = ?", String.class, e.companyId())
                        + "\",\"address\":\"Control 123\",\"slug\":\"" + e.slug() + "\"}",
                "{\"name\":\"Intruso\",\"phone\":\"999000111\",\"email\":\"intruso@intruso.test\",\"ruc\":\"20111111111\",\"address\":\"Intruso 666\",\"slug\":\"intruso\"}",
                "company", (long) e.companyId(), dueno, intrusosStaff));
        intentos.add(new Intento("editar el rol ajeno", "PUT", "/admin/roles/" + e.rolPersonalizadoId(),
                "{\"nombre\":\"Rol personalizado " + m + "\",\"descripcion\":\"Control\",\"scope\":\"STAFF\"}",
                "{\"nombre\":\"Intruso\",\"descripcion\":\"Intruso\",\"scope\":\"STAFF\"}",
                "roles", (long) e.rolPersonalizadoId(), dueno, intrusosStaff));
        intentos.add(new Intento("agendar una cita para la mascota ajena", "POST", "/appointments",
                "{\"mascotaId\":" + e.mascota1Id() + ",\"veterinarioId\":" + e.empleadoId() + ",\"servicioId\":" + e.servicioId()
                        + ",\"motivoCita\":\"Control\",\"fechaHoraInicio\":\"" + futuro + "\",\"esEmergencia\":true}",
                "{\"mascotaId\":" + e.mascota1Id() + ",\"veterinarioId\":" + e.empleadoId() + ",\"servicioId\":" + e.servicioId()
                        + ",\"motivoCita\":\"Intruso\",\"fechaHoraInicio\":\"" + futuro2 + "\",\"esEmergencia\":true}",
                "citas", null, dueno, intrusosStaff));
        intentos.add(new Intento("reprogramar la cita ajena", "PATCH", "/appointments/" + e.citaFuturaId() + "/reschedule",
                "{\"veterinarioId\":" + e.empleadoId() + ",\"fechaHoraInicio\":\"" + futuro.replace("T10:00", "T15:00") + "\",\"motivoReprogramacion\":\"Control de la propia clínica\"}",
                "{\"veterinarioId\":" + e.empleadoId() + ",\"fechaHoraInicio\":\"" + futuro2.replace("T11:00", "T16:00") + "\",\"motivoReprogramacion\":\"Intruso de otra clínica\"}",
                "citas", e.citaFuturaId(), dueno, intrusosStaff));
        intentos.add(new Intento("editar la mascota de otro cliente desde el portal", "PUT", "/clients/portal/pets/" + e.mascota1Id(),
                "{\"fotoUrl\":\"https://ejemplo.test/control.png\"}", "{\"fotoUrl\":\"https://intruso.test/intruso.png\"}",
                "mascota", e.mascota1Id(), duenoCliente, intrusosCliente));
        return intentos;
    }

    private void abrirClinicaYPublicarAviso(DosEmpresasSembradas.Empresa e) {
        jdbc.update("UPDATE rol_vista_permisos SET modificar = TRUE WHERE vista_id = (SELECT id FROM vistas WHERE codigo = 'VISTA_MIS_MASCOTAS') "
                + "AND rol_id IN (SELECT id FROM roles WHERE company_id = ? AND name = 'ROLE_CLIENTE')", e.companyId());
        Integer horarios = jdbc.queryForObject("SELECT COUNT(*) FROM company_operating_hours WHERE company_id = ?", Integer.class, e.companyId());
        if (horarios == 0) {
            for (String dia : List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")) {
                jdbc.update("INSERT INTO company_operating_hours (company_id, dia_semana, opening_time, closing_time, is_open) VALUES (?, ?, '08:00:00', '20:00:00', TRUE)",
                        e.companyId(), dia);
            }
        }
        Integer avisos = jdbc.queryForObject("SELECT COUNT(*) FROM aviso_privacidad WHERE company_id = ?", Integer.class, e.companyId());
        if (avisos == 0) {
            jdbc.update("INSERT INTO aviso_privacidad (company_id, audiencia, version, contenido, contenido_hash, campos, vigente_desde, activo, creado_en) "
                    + "VALUES (?, 'PROPIETARIOS_Y_AUTORIZADOS', 1, 'Aviso de prueba', 'hash-" + e.marca() + "', 'nombre,correo', CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP)", e.companyId());
            jdbc.update("INSERT INTO consentimiento_datos (company_id, usuario_id, aviso_id, aviso_version, contenido_hash, finalidad, estado, canal, fecha) "
                    + "SELECT ?, u.id, (SELECT id FROM aviso_privacidad WHERE company_id = ?), 1, 'hash-" + e.marca() + "', 'ENTERADO', 'OTORGADO', 'PORTAL', CURRENT_TIMESTAMP "
                    + "FROM usuario u WHERE u.company_id = ? AND u.username LIKE 'cliente%'", e.companyId(), e.companyId(), e.companyId());
        }
    }

    private Long marcaDe(DosEmpresasSembradas.Empresa e) {
        List<Long> existentes = jdbc.queryForList("SELECT id FROM marca_producto WHERE company_id = ?", Long.class, e.companyId());
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        jdbc.update("INSERT INTO marca_producto (nombre, activo, company_id) VALUES (?, TRUE, ?)", "Marca " + e.marca(), e.companyId());
        return jdbc.queryForObject("SELECT id FROM marca_producto WHERE company_id = ?", Long.class, e.companyId());
    }

    private void darTurno(DosEmpresasSembradas.Empresa e, java.time.LocalDateTime dia) {
        String[] nombres = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};
        jdbc.update("INSERT INTO horario_empleado (empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo) VALUES (?, ?, ?, '08:00:00', '20:00:00', TRUE)",
                e.empleadoId(), dia.toLocalDate(), nombres[dia.getDayOfWeek().getValue() - 1]);
    }

    private Long razaDe(DosEmpresasSembradas.Empresa e) {
        abrirClinicaYPublicarAviso(e);
        List<Long> existentes = jdbc.queryForList("SELECT id FROM raza WHERE company_id = ?", Long.class, e.companyId());
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        jdbc.update("INSERT INTO raza (nombre, especie, activo, company_id) VALUES (?, 'PERRO', TRUE, ?)", "Raza " + e.marca(), e.companyId());
        return jdbc.queryForObject("SELECT id FROM raza WHERE company_id = ?", Long.class, e.companyId());
    }

    private String digitosDe(String marca) {
        return marca.equals("TKA") ? "1000001" : "2000002";
    }

    @Test
    @Order(30)
    void crearYEditarConDatosValidosEnLoAjenoNoLogra() {
        List<String> violaciones = new ArrayList<>();
        List<Intento> intentos = new ArrayList<>();
        intentos.addAll(intentosValidos(a, adminA, List.of(adminB, clienteB, sinPermisosB), cliente1A, List.of(cliente2A, clienteB)));
        intentos.addAll(intentosValidos(b, adminB, List.of(adminA, cliente1A, sinPermisosA), clienteB, List.of(cliente2A, cliente1A)));

        List<String> controlesInvalidos = new ArrayList<>();
        for (Intento intento : intentos) {
            Respuesta control = llamar(intento.dueno(), intento.verbo(), intento.ruta(), intento.cuerpoControl());
            if (!control.exito()) {
                controlesInvalidos.add(intento.nombre() + " (dueño) -> " + control.estado() + " " + recortar(control.cuerpo()));
            }
        }
        assertThat(controlesInvalidos).as("El dueño legítimo debe poder hacer cada operación, o la prueba no demuestra nada").isEmpty();

        for (Intento intento : intentos) {
            for (Sesion intruso : intento.intrusos()) {
                long antesFilas = contar(intento.tabla());
                String antesFila = intento.idFila() == null ? "" : huellaDe(intento.tabla(), intento.idFila());
                Respuesta respuesta = llamar(intruso, intento.verbo(), intento.ruta(), intento.cuerpoIntruso());
                long despuesFilas = contar(intento.tabla());
                String despuesFila = intento.idFila() == null ? "" : huellaDe(intento.tabla(), intento.idFila());
                if (respuesta.exito()) {
                    violaciones.add(intruso.nombre() + " logró \"" + intento.nombre() + "\" -> " + respuesta.estado());
                }
                if (respuesta.estado() >= 500) {
                    violaciones.add(intruso.nombre() + " provocó un error interno con \"" + intento.nombre() + "\" -> " + respuesta.estado());
                }
                if (antesFilas != despuesFilas) {
                    violaciones.add(intruso.nombre() + " cambió la cantidad de filas de " + intento.tabla() + " con \"" + intento.nombre() + "\"");
                }
                if (!antesFila.equals(despuesFila)) {
                    violaciones.add(intruso.nombre() + " modificó " + intento.tabla() + "#" + intento.idFila() + " con \"" + intento.nombre() + "\"");
                }
            }
        }

        assertThat(violaciones).as("Escrituras válidas que lograron algo en lo ajeno").isEmpty();
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class);
    }

    private String huellaDe(String tabla, Long id) {
        return huella(List.of(new DosEmpresasSembradas.Fila(tabla, id, "x"))).values().iterator().next();
    }

    private String recortar(String texto) {
        return texto.length() > 160 ? texto.substring(0, 160) : texto;
    }

    // ------------------------------------------------------------------ apoyo

    private List<CatalogoEndpoints.Endpoint> lecturasPorId() {
        return endpoints.stream()
                .filter(e -> e.verbo().equals("GET") && e.tieneVariables() && !e.esPublico() && !e.esAutoservicio()).toList();
    }

    private void verificarSinPermiso(Sesion sesion, DosEmpresasSembradas.Empresa propia,
                                     Predicate<CatalogoEndpoints.Endpoint> permitido, List<String> violaciones) {
        for (CatalogoEndpoints.Endpoint endpoint : endpoints) {
            if (endpoint.esPublico() || endpoint.ruta().startsWith("/auth/") || endpoint.ruta().startsWith("/realtime")
                    || endpoint.ruta().startsWith("/media") || permitido.test(endpoint)) continue;
            for (String url : urls(endpoint, propia, 1)) {
                Respuesta respuesta = llamar(sesion, endpoint.verbo(), url, cuerpoPara(endpoint));
                if (respuesta.exito()) {
                    violaciones.add(sesion.nombre() + " " + endpoint.descripcion() + " -> " + respuesta.estado());
                }
            }
        }
    }

    private void atacarYComparar(List<Sesion> intrusos, List<CatalogoEndpoints.Endpoint> escrituras,
                                 DosEmpresasSembradas.Empresa objetivo, List<DosEmpresasSembradas.Fila> protegidas,
                                 List<String> violaciones) {
        for (Sesion intruso : intrusos) {
            Map<String, String> antes = huella(protegidas);
            for (CatalogoEndpoints.Endpoint endpoint : escrituras) {
                for (String url : urls(endpoint, objetivo, MAX_ID)) {
                    llamar(intruso, endpoint.verbo(), url, cuerpoPara(endpoint));
                }
            }
            Map<String, String> despues = huella(protegidas);
            for (var fila : antes.entrySet()) {
                if (!fila.getValue().equals(despues.get(fila.getKey()))) {
                    violaciones.add(intruso.nombre() + " modificó " + fila.getKey());
                }
            }
        }
    }

    private List<String> buscarFugas(Sesion intruso, List<CatalogoEndpoints.Endpoint> lecturas,
                                     DosEmpresasSembradas.Empresa objetivo, List<String> prohibidos) {
        List<String> violaciones = new ArrayList<>();
        for (CatalogoEndpoints.Endpoint endpoint : lecturas) {
            for (String url : urls(endpoint, objetivo, MAX_ID)) {
                Respuesta respuesta = llamar(intruso, "GET", url, null);
                for (String prohibido : prohibidos) {
                    if (respuesta.cuerpo().contains(prohibido)) {
                        violaciones.add(intruso.nombre() + " " + url + " -> " + respuesta.estado() + " muestra " + prohibido);
                    }
                }
            }
        }
        return violaciones;
    }

    private int contarAciertos(Sesion dueno, List<CatalogoEndpoints.Endpoint> lecturas, DosEmpresasSembradas.Empresa propia,
                               String marca) {
        int aciertos = 0;
        for (CatalogoEndpoints.Endpoint endpoint : lecturas) {
            for (String url : urls(endpoint, propia, MAX_ID)) {
                Respuesta respuesta = llamar(dueno, "GET", url, null);
                if (respuesta.exito() && respuesta.cuerpo().contains(marca)) {
                    aciertos++;
                    break;
                }
            }
        }
        return aciertos;
    }

    private int contarAciertosListados(Sesion dueno, List<CatalogoEndpoints.Endpoint> listados, String marca) {
        int aciertos = 0;
        for (CatalogoEndpoints.Endpoint endpoint : listados) {
            Respuesta respuesta = llamar(dueno, "GET", endpoint.ruta(), null);
            if (respuesta.exito() && respuesta.cuerpo().contains(marca)) aciertos++;
        }
        return aciertos;
    }

    private List<String> privadosDelCliente1() {
        return List.of("AnaTKA", "LunaTKA", "cliente1.tka", "HC-TKA-1", "Motivo confidencial TKA", "Diagnostico TKA",
                "Tratamiento TKA", "Medicamento TKA");
    }

    private List<String> privadosDelCliente2() {
        return List.of("BeatrizTKA", "NubeTKA", "cliente2.tka");
    }

    /** Las URL de un endpoint con identificadores de la clínica objetivo: los números 1..maximo y los ids reales de sus datos. */
    private List<String> urls(CatalogoEndpoints.Endpoint endpoint, DosEmpresasSembradas.Empresa objetivo, int maximo) {
        List<String> urls = new ArrayList<>();
        if (!endpoint.tieneVariables()) {
            urls.add(endpoint.ruta());
            return urls;
        }
        for (int i = 1; i <= maximo; i++) {
            final int n = i;
            urls.add(CatalogoEndpoints.sustituir(endpoint.ruta(), variable -> valorDe(variable, n, objetivo)));
        }
        urls.add(CatalogoEndpoints.sustituir(endpoint.ruta(), variable -> valorDe(variable, 0, objetivo)));
        return urls;
    }

    private String valorDe(String variable, int n, DosEmpresasSembradas.Empresa objetivo) {
        boolean real = n == 0;
        return switch (variable) {
            case "uuid", "mascotaUuid" -> objetivo.uuidMascota1();
            case "relacionUuid" -> UUID.randomUUID().toString();
            case "sku" -> objetivo.sku();
            case "numeroHc" -> objetivo.numeroHc();
            case "slug" -> objetivo.slug();
            case "finalidad" -> "USO_IA_CLINICA";
            case "filename" -> "x.png";
            case "petId", "mascotaId" -> String.valueOf(real ? objetivo.mascota1Id() : n);
            case "consultationId" -> String.valueOf(real ? objetivo.consultaId() : n);
            case "citaId", "appointmentId" -> String.valueOf(real ? objetivo.citaId() : n);
            case "employeeId" -> String.valueOf(real ? objetivo.empleadoId() : n);
            default -> String.valueOf(real ? objetivo.citaId() : n);
        };
    }

    private String cuerpoPara(CatalogoEndpoints.Endpoint endpoint) {
        return switch (endpoint.verbo()) {
            case "POST", "PUT", "PATCH" -> "{}";
            default -> null;
        };
    }

    private List<DosEmpresasSembradas.Fila> filasDe(DosEmpresasSembradas.Empresa empresa, String... grupos) {
        List<String> lista = List.of(grupos);
        return empresa.filas().stream().filter(f -> lista.contains(f.grupo())).toList();
    }

    private Map<String, String> huella(List<DosEmpresasSembradas.Fila> filas) {
        Map<String, String> huella = new TreeMap<>();
        for (DosEmpresasSembradas.Fila fila : filas) {
            List<Map<String, Object>> filasBd = jdbc.queryForList("SELECT * FROM " + fila.tabla() + " WHERE id = ?", fila.id());
            huella.put(fila.tabla() + "#" + fila.id(), filasBd.isEmpty() ? "AUSENTE" : new TreeMap<>(filasBd.get(0)).toString());
        }
        return huella;
    }

    private Sesion iniciarSesion(String nombre, String slug, String usuario) throws Exception {
        MvcResult resultado = mvc.perform(request(HttpMethod.POST, "/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("X-Company-Slug", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"" + slug + "\",\"username\":\"" + usuario + "\",\"password\":\""
                                + DosEmpresasSembradas.CLAVE + "\"}"))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).as("inicio de sesión de " + usuario).isEqualTo(200);
        return new Sesion(nombre, slug, resultado.getResponse().getCookies());
    }

    private Respuesta llamar(Sesion sesion, String verbo, String ruta, String cuerpo) {
        try {
            MockHttpServletRequestBuilder peticion = request(HttpMethod.valueOf(verbo), ruta)
                    .header("Origin", "http://localhost:4200");
            if (sesion != null) {
                peticion.header("X-Company-Slug", sesion.slug()).cookie(sesion.cookies());
            }
            if (cuerpo != null) {
                peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
            }
            MvcResult resultado = mvc.perform(peticion).andReturn();
            return new Respuesta(resultado.getResponse().getStatus(), resultado.getResponse().getContentAsString());
        } catch (Exception ex) {
            return new Respuesta(500, "excepción: " + ex.getClass().getSimpleName());
        }
    }
}
