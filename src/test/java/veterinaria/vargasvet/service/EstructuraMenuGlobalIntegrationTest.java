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
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.VistaReorderDTO;
import veterinaria.vargasvet.dto.response.MenuStructureDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({MenuBuilderService.class, VistaService.class})
class EstructuraMenuGlobalIntegrationTest {

    @Autowired private TestEntityManager em;
    @Autowired private MenuBuilderService menu;
    @Autowired private VistaService vistaService;
    @MockitoBean private AuditLogService auditLogService;

    private Usuario usuario;
    private Role rol;
    private Ventana clinica;
    private Ventana administracion;
    private Vista clientes;
    private Vista mascotas;
    private Vista roles;
    private Vista perfil;

    @BeforeEach
    void datos() {
        usuario = new Usuario();
        usuario.setEmail("ana-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("ana-" + UUID.randomUUID());
        usuario = em.persist(usuario);

        rol = new Role();
        rol.setName("Recepción " + UUID.randomUUID());
        rol.setScope(RoleScope.STAFF);
        rol.setPurpose(RolePurpose.CUSTOM);
        rol = em.persist(rol);

        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        em.persist(asignacion);

        clinica = ventana("CLINICA-" + UUID.randomUUID(), "Clínica", 20, MenuPresentation.GROUPED);
        administracion = ventana("ADMIN-" + UUID.randomUUID(), "Administración", 10, MenuPresentation.GROUPED);

        clientes = vista("VISTA_CLIENTES", "Clientes", clinica, 10);
        mascotas = vista("VISTA_MASCOTAS", "Mascotas", clinica, 20);
        roles = vista("VISTA_ROLES", "Roles", administracion, 10);
        perfil = vista("VISTA_PROFILE", "Mi perfil", null, 98);
        em.flush();
        em.clear();
    }

    private List<MenuStructureDTO> navegacion() {
        return menu.construirMenuJerarquico(usuario.getId(), rol.getId(), null);
    }

    private List<String> codigos(List<MenuStructureDTO> estructura) {
        return estructura.stream().flatMap(s -> s.getVistas().stream()).map(v -> v.getCodigo()).toList();
    }

    private void reordenar(Vista vista, int orden) {
        VistaReorderDTO cambio = new VistaReorderDTO();
        cambio.setId(vista.getId());
        cambio.setOrden(orden);
        vistaService.reordenar(List.of(cambio));
        em.flush();
        em.clear();
    }

    @Test
    void losModulosSeMuestranEnElOrdenConfiguradoYSusVistasEnSuPosicion() {
        assertThat(codigos(navegacion()))
                .containsExactly("VISTA_ROLES", "VISTA_CLIENTES", "VISTA_MASCOTAS", "VISTA_PROFILE");
    }

    @Test
    void laPosicionGuardadaDeUnaVistaSeConservaYSeReflejaEnLaNavegacion() {
        reordenar(mascotas, 5);

        assertThat(codigos(navegacion()))
                .containsExactly("VISTA_ROLES", "VISTA_MASCOTAS", "VISTA_CLIENTES", "VISTA_PROFILE");
        assertThat(em.find(Vista.class, mascotas.getId()).getOrden()).isEqualTo(5);
    }

    @Test
    void laAgrupacionPorModuloSeConservaTrasCambiarLaPosicion() {
        reordenar(clientes, 30);

        var estructura = navegacion();
        var modulo = estructura.stream().filter(s -> "Clínica".equals(s.getVentanaNombre())).findFirst().orElseThrow();
        assertThat(modulo.getPresentacion()).isEqualTo(MenuPresentation.GROUPED);
        assertThat(modulo.getVistas().stream().map(v -> v.getCodigo()).toList())
                .containsExactly("VISTA_MASCOTAS", "VISTA_CLIENTES");
    }

    @Test
    void unModuloPlanoMuestraCadaVistaComoEntradaPropiaSinPerderSuPosicion() {
        Ventana plano = em.find(Ventana.class, clinica.getId());
        plano.setPresentacionDefault(MenuPresentation.FLAT);
        em.flush();
        em.clear();

        var estructura = navegacion();

        var delModulo = estructura.stream().filter(s -> "Clínica".equals(s.getVentanaNombre())).toList();
        assertThat(delModulo).hasSize(2).allSatisfy(s -> assertThat(s.getPresentacion()).isEqualTo(MenuPresentation.FLAT));
        assertThat(delModulo.stream().map(s -> s.getVistas().get(0).getCodigo()).toList())
                .containsExactly("VISTA_CLIENTES", "VISTA_MASCOTAS");
    }

    @Test
    void cambiarElOrdenDeUnModuloMueveTodasSusVistas() {
        Ventana modulo = em.find(Ventana.class, clinica.getId());
        modulo.setOrden(1);
        em.flush();
        em.clear();

        assertThat(codigos(navegacion()))
                .containsExactly("VISTA_CLIENTES", "VISTA_MASCOTAS", "VISTA_ROLES", "VISTA_PROFILE");
    }

    @Test
    void laConfiguracionEsLaMismaParaCualquierUsuarioDelMismoRol() {
        Usuario otra = new Usuario();
        otra.setEmail("beto-" + UUID.randomUUID() + "@patitas.test");
        otra.setUsername("beto-" + UUID.randomUUID());
        otra = em.persist(otra);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(otra);
        asignacion.setRol(em.find(Role.class, rol.getId()));
        em.persist(asignacion);
        em.flush();
        em.clear();

        assertThat(codigos(menu.construirMenuJerarquico(otra.getId(), rol.getId(), null)))
                .isEqualTo(codigos(navegacion()));
    }

    private VistaReorderDTO cambio(Vista vista, int orden) {
        VistaReorderDTO cambio = new VistaReorderDTO();
        cambio.setId(vista.getId());
        cambio.setOrden(orden);
        return cambio;
    }

    private List<String> vistasDelModulo(List<MenuStructureDTO> estructura, String nombre) {
        return estructura.stream().filter(s -> nombre.equals(s.getVentanaNombre()))
                .flatMap(s -> s.getVistas().stream()).map(v -> v.getCodigo()).toList();
    }

    @Test
    void moverUnaVistaDeModuloLaMuestraEnElModuloDestinoYLaQuitaDelOrigen() {
        VistaReorderDTO mover = cambio(clientes, 20);
        mover.setVentanaId(administracion.getId());

        vistaService.reordenar(List.of(mover));
        em.flush();
        em.clear();

        var estructura = navegacion();
        assertThat(vistasDelModulo(estructura, "Administración")).containsExactly("VISTA_ROLES", "VISTA_CLIENTES");
        assertThat(vistasDelModulo(estructura, "Clínica")).containsExactly("VISTA_MASCOTAS");
        assertThat(em.find(Vista.class, clientes.getId()).getVentana().getId()).isEqualTo(administracion.getId());
    }

    @Test
    void dejarUnaVistaSinModuloLaMuestraComoEntradaSuelta() {
        VistaReorderDTO soltar = cambio(mascotas, 50);
        soltar.setSinModulo(true);

        vistaService.reordenar(List.of(soltar));
        em.flush();
        em.clear();

        var estructura = navegacion();
        var suelta = estructura.stream().filter(s -> s.getVentanaId() == null)
                .flatMap(s -> s.getVistas().stream()).map(v -> v.getCodigo()).toList();
        assertThat(suelta).contains("VISTA_MASCOTAS");
        assertThat(vistasDelModulo(estructura, "Clínica")).containsExactly("VISTA_CLIENTES");
        assertThat(em.find(Vista.class, mascotas.getId()).getVentana()).isNull();
    }

    @Test
    void unaSolicitudSinIndicarModuloConservaElModuloActual() {
        vistaService.reordenar(List.of(cambio(clientes, 15)));
        em.flush();
        em.clear();

        assertThat(em.find(Vista.class, clientes.getId()).getVentana().getId()).isEqualTo(clinica.getId());
        assertThat(em.find(Vista.class, clientes.getId()).getOrden()).isEqualTo(15);
    }

    @Test
    void siUnCambioEsInvalidoNoSeAplicaNingunoDeLosDemas() {
        VistaReorderDTO valido = cambio(clientes, 99);
        valido.setVentanaId(administracion.getId());
        VistaReorderDTO inexistente = cambio(mascotas, 10);
        inexistente.setVentanaId(987654);

        assertThatThrownBy(() -> vistaService.reordenar(List.of(valido, inexistente)))
                .isInstanceOf(ResourceNotFoundException.class);
        em.flush();
        em.clear();

        Vista intacta = em.find(Vista.class, clientes.getId());
        assertThat(intacta.getOrden()).isEqualTo(10);
        assertThat(intacta.getVentana().getId()).isEqualTo(clinica.getId());
    }

    @Test
    void rechazaOrdenesInvalidasVistasRepetidasONingunCambio() {
        assertThatThrownBy(() -> vistaService.reordenar(List.of(cambio(clientes, -1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("orden");
        assertThatThrownBy(() -> vistaService.reordenar(List.of(cambio(clientes, 10001))))
                .isInstanceOf(IllegalArgumentException.class);
        VistaReorderDTO sinOrden = cambio(clientes, 0);
        sinOrden.setOrden(null);
        assertThatThrownBy(() -> vistaService.reordenar(List.of(sinOrden)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> vistaService.reordenar(List.of(cambio(clientes, 10), cambio(clientes, 20))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("repetida");
        assertThatThrownBy(() -> vistaService.reordenar(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> vistaService.reordenar(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermiteIndicarUnModuloYASuVezDejarLaVistaSinModulo() {
        VistaReorderDTO contradictorio = cambio(clientes, 10);
        contradictorio.setVentanaId(administracion.getId());
        contradictorio.setSinModulo(true);

        assertThatThrownBy(() -> vistaService.reordenar(List.of(contradictorio)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnaVistaQueNoExiste() {
        VistaReorderDTO fantasma = new VistaReorderDTO();
        fantasma.setId(987654);
        fantasma.setOrden(10);

        assertThatThrownBy(() -> vistaService.reordenar(List.of(fantasma)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elGrupoHeredadoSigueSiendoOpcionalYSeValida() {
        VistaReorderDTO conGrupo = cambio(clientes, 10);
        conGrupo.setGrupo("CLINICA");
        vistaService.reordenar(List.of(conGrupo));
        assertThat(em.find(Vista.class, clientes.getId()).getGrupo()).isEqualTo("CLINICA");

        VistaReorderDTO invalido = cambio(clientes, 10);
        invalido.setGrupo("<script>");
        assertThatThrownBy(() -> vistaService.reordenar(List.of(invalido)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Ventana ventana(String codigo, String nombre, int orden, MenuPresentation presentacion) {
        Ventana ventana = new Ventana();
        ventana.setCodigo(codigo);
        ventana.setNombre(nombre);
        ventana.setOrden(orden);
        ventana.setActivo(true);
        ventana.setPresentacionDefault(presentacion);
        return em.persist(ventana);
    }

    private Vista vista(String codigo, String nombre, Ventana ventana, int orden) {
        Vista vista = new Vista();
        vista.setCodigo(codigo);
        vista.setNombre(nombre);
        vista.setRuta("/" + nombre.toLowerCase());
        vista.setOrden(orden);
        vista.setVentana(ventana);
        vista = em.persist(vista);
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setRol(em.find(Role.class, rol.getId()));
        permiso.setVista(vista);
        permiso.setLeer(true);
        em.persist(permiso);
        return vista;
    }
}
