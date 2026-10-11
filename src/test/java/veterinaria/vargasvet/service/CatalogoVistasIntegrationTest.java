package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.VistaReorderDTO;
import veterinaria.vargasvet.dto.request.VistaRequestDTO;
import veterinaria.vargasvet.dto.response.VistaDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import({MenuBuilderService.class, VistaService.class})
class CatalogoVistasIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private VistaService vistaService;
    @Autowired private VistaRepository vistaRepository;
    @Autowired private MenuBuilderService menu;
    @MockitoBean private AuditLogService auditLogService;

    private Ventana clinica;
    private Ventana administracion;

    @BeforeEach
    void modulos() {
        clinica = modulo("CLINICA", "Clínica", 2);
        administracion = modulo("ADMIN", "Administración", 1);
        em.flush();
    }

    private Ventana modulo(String codigo, String nombre, int orden) {
        Ventana ventana = new Ventana();
        ventana.setCodigo(codigo + "-" + UUID.randomUUID());
        ventana.setNombre(nombre);
        ventana.setOrden(orden);
        ventana.setActivo(true);
        ventana.setPresentacionDefault(MenuPresentation.GROUPED);
        return em.persist(ventana);
    }

    private VistaRequestDTO solicitud(String codigo, String nombre) {
        VistaRequestDTO request = new VistaRequestDTO();
        request.setCodigo(codigo);
        request.setNombre(nombre);
        request.setGrupo("CLINICA");
        request.setOrden(10);
        request.setVentanaId(clinica.getId());
        request.setActivo(true);
        return request;
    }

    @Test
    void registrarUnaVistaLaDejaConsultableConSuModuloYSuPosicion() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        em.flush();
        em.clear();

        VistaDTO guardada = vistaService.listarTodas().stream()
                .filter(v -> v.getId().equals(creada.getId())).findFirst().orElseThrow();
        assertThat(guardada.getCodigo()).isEqualTo("VISTA_VACUNAS");
        assertThat(guardada.getNombre()).isEqualTo("Vacunas");
        assertThat(guardada.getVentanaId()).isEqualTo(clinica.getId());
        assertThat(guardada.getOrden()).isEqualTo(10);
        assertThat(guardada.isActivo()).isTrue();
    }

    @Test
    void elCodigoSeNormalizaYNoPermiteDuplicadosAunqueSeEscribaDistinto() {
        vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        em.flush();

        assertThatThrownBy(() -> vistaService.crear(solicitud("vista vacunas", "Otra vez")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void noPermiteRegistrarDosVecesElMismoCodigo() {
        vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        em.flush();

        assertThatThrownBy(() -> vistaService.crear(solicitud("VISTA_VACUNAS", "Duplicada")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermiteRegistrarUnaVistaEnUnModuloQueNoExiste() {
        VistaRequestDTO request = solicitud("VISTA_FANTASMA", "Fantasma");
        request.setVentanaId(987654);

        assertThatThrownBy(() -> vistaService.crear(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void consultarDevuelveTambienLasRetiradasYPermiteFiltrarPorGrupo() {
        VistaDTO a = vistaService.crear(solicitud("VISTA_A", "A"));
        VistaRequestDTO otraGrupo = solicitud("VISTA_B", "B");
        otraGrupo.setGrupo("ADMIN");
        vistaService.crear(otraGrupo);
        vistaService.eliminar(a.getId());
        em.flush();
        em.clear();

        assertThat(vistaService.listarTodas()).extracting(VistaDTO::getCodigo).contains("VISTA_A", "VISTA_B");
        assertThat(vistaService.listarPorGrupo("ADMIN")).extracting(VistaDTO::getCodigo).containsExactly("VISTA_B");
    }

    @Test
    void modificarCambiaNombreModuloIconoYEstadoPeroNoElCodigo() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        VistaRequestDTO cambios = solicitud("OTRO_CODIGO_IGNORADO", "Vacunación");
        cambios.setVentanaId(administracion.getId());
        cambios.setIcono("pi-heart");
        cambios.setOrden(30);
        cambios.setActivo(false);

        vistaService.actualizar(creada.getId(), cambios);
        em.flush();
        em.clear();

        var guardada = vistaRepository.findById(creada.getId()).orElseThrow();
        assertThat(guardada.getCodigo()).isEqualTo("VISTA_VACUNAS");
        assertThat(guardada.getNombre()).isEqualTo("Vacunación");
        assertThat(guardada.getVentana().getId()).isEqualTo(administracion.getId());
        assertThat(guardada.getIcono()).isEqualTo("pi-heart");
        assertThat(guardada.getOrden()).isEqualTo(30);
        assertThat(guardada.isActivo()).isFalse();
    }

    @Test
    void modificarUnaVistaOUnModuloQueNoExistenSeRechaza() {
        assertThatThrownBy(() -> vistaService.actualizar(987654, solicitud("X", "X")))
                .isInstanceOf(ResourceNotFoundException.class);
        VistaDTO creada = vistaService.crear(solicitud("VISTA_A", "A"));
        VistaRequestDTO cambios = solicitud("VISTA_A", "A");
        cambios.setVentanaId(987654);
        assertThatThrownBy(() -> vistaService.actualizar(creada.getId(), cambios))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retirarUnaVistaLaConservaEnElCatalogoPeroLaQuitaDeLaNavegacion() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("ana-" + UUID.randomUUID());
        usuario = em.persist(usuario);
        Role rol = new Role();
        rol.setName("Recepción " + UUID.randomUUID());
        rol.setScope(RoleScope.STAFF);
        rol.setPurpose(RolePurpose.CUSTOM);
        rol = em.persist(rol);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        em.persist(asignacion);
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setRol(rol);
        permiso.setVista(vistaRepository.findById(creada.getId()).orElseThrow());
        permiso.setLeer(true);
        em.persist(permiso);
        em.flush();
        em.clear();
        assertThat(menu.construirPermissions(usuario.getId(), rol.getId(), null)).contains("VISTA_VACUNAS:LEER");

        vistaService.eliminar(creada.getId());
        em.flush();
        em.clear();

        assertThat(vistaRepository.findById(creada.getId())).isPresent();
        assertThat(vistaRepository.findById(creada.getId()).orElseThrow().isActivo()).isFalse();
        assertThat(menu.construirMenu(usuario.getId(), rol.getId(), null)).isEmpty();
    }

    @Test
    void retirarUnaVistaInexistenteSeRechaza() {
        assertThatThrownBy(() -> vistaService.eliminar(987654)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unaVistaRetiradaSePuedeReactivar() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        vistaService.eliminar(creada.getId());
        VistaRequestDTO reactivar = solicitud("VISTA_VACUNAS", "Vacunas");
        reactivar.setActivo(true);

        vistaService.actualizar(creada.getId(), reactivar);
        em.flush();
        em.clear();

        assertThat(vistaRepository.findById(creada.getId()).orElseThrow().isActivo()).isTrue();
    }

    private static final String MODULO = "Catálogo de vistas";

    @Test
    void registrarUnaVistaDejaConstanciaConSuCodigoModuloYPosicion() {
        vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));

        verify(auditLogService).log(eq("REGISTRAR_VISTA"), eq(MODULO),
                contains("VISTA_VACUNAS \"Vacunas\" en Clínica, posición 10"));
    }

    @Test
    void unRegistroRechazadoNoDejaConstancia() {
        vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        em.flush();

        assertThatThrownBy(() -> vistaService.crear(solicitud("vista vacunas", "Otra")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(auditLogService, times(1)).log(eq("REGISTRAR_VISTA"), anyString(), anyString());
    }

    @Test
    void modificarDejaConstanciaDeLoQueCambioConSuValorAnteriorYNuevo() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));
        VistaRequestDTO cambios = solicitud("VISTA_VACUNAS", "Vacunación");
        cambios.setVentanaId(administracion.getId());
        cambios.setOrden(30);
        cambios.setActivo(false);

        vistaService.actualizar(creada.getId(), cambios);

        org.mockito.ArgumentCaptor<String> detalle = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("MODIFICAR_VISTA"), eq(MODULO), detalle.capture());
        assertThat(detalle.getValue())
                .contains("VISTA_VACUNAS")
                .contains("nombre \"Vacunas\" → \"Vacunación\"")
                .contains("módulo Clínica → Administración")
                .contains("posición 10 → 30")
                .contains("retirada");
    }

    @Test
    void guardarSinCambiosNoGeneraRuido() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));

        vistaService.actualizar(creada.getId(), solicitud("VISTA_VACUNAS", "Vacunas"));

        verify(auditLogService, never()).log(eq("MODIFICAR_VISTA"), anyString(), anyString());
    }

    @Test
    void retirarDejaConstanciaUnaSolaVezAunqueSeRepita() {
        VistaDTO creada = vistaService.crear(solicitud("VISTA_VACUNAS", "Vacunas"));

        vistaService.eliminar(creada.getId());
        vistaService.eliminar(creada.getId());

        verify(auditLogService, times(1)).log(eq("RETIRAR_VISTA"), eq(MODULO),
                contains("VISTA_VACUNAS \"Vacunas\""));
    }

    @Test
    void reordenarDejaConstanciaDeLasVistasQueCambiaronDeModulo() {
        VistaDTO a = vistaService.crear(solicitud("VISTA_A", "A"));
        VistaDTO b = vistaService.crear(solicitud("VISTA_B", "B"));
        VistaReorderDTO mover = new VistaReorderDTO();
        mover.setId(a.getId());
        mover.setOrden(10);
        mover.setVentanaId(administracion.getId());
        VistaReorderDTO quedarse = new VistaReorderDTO();
        quedarse.setId(b.getId());
        quedarse.setOrden(20);

        vistaService.reordenar(List.of(mover, quedarse));

        org.mockito.ArgumentCaptor<String> detalle = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("REORDENAR_VISTAS"), eq(MODULO), detalle.capture());
        assertThat(detalle.getValue()).contains("2 vista(s)").contains("VISTA_A: Clínica → Administración")
                .doesNotContain("VISTA_B:");
    }

    @Test
    void reordenarSinNingunCambioRealNoGeneraConstancia() {
        VistaDTO a = vistaService.crear(solicitud("VISTA_A", "A"));
        VistaReorderDTO igual = new VistaReorderDTO();
        igual.setId(a.getId());
        igual.setOrden(10);

        vistaService.reordenar(List.of(igual));

        verify(auditLogService, never()).log(eq("REORDENAR_VISTAS"), anyString(), anyString());
    }

    @Test
    void unReordenamientoRechazadoNoDejaConstancia() {
        VistaDTO a = vistaService.crear(solicitud("VISTA_A", "A"));
        VistaReorderDTO invalido = new VistaReorderDTO();
        invalido.setId(a.getId());
        invalido.setOrden(-1);

        assertThatThrownBy(() -> vistaService.reordenar(List.of(invalido)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(auditLogService, never()).log(eq("REORDENAR_VISTAS"), anyString(), anyString());
    }

    @Test
    void cadaVistaConservaLaPosicionQueSeLeDefine() {
        VistaRequestDTO primera = solicitud("VISTA_UNO", "Uno");
        primera.setOrden(20);
        VistaRequestDTO segunda = solicitud("VISTA_DOS", "Dos");
        segunda.setOrden(10);
        vistaService.crear(primera);
        vistaService.crear(segunda);
        em.flush();
        em.clear();

        List<String> enOrden = vistaService.listarTodas().stream()
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden())).map(VistaDTO::getCodigo).toList();
        assertThat(enOrden).containsSubsequence("VISTA_DOS", "VISTA_UNO");
    }
}
