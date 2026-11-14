package veterinaria.vargasvet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.DataScope;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/** Leer los permisos del rol una vez por solicitud no cambia ninguna decisión de acceso. */
@ExtendWith(MockitoExtension.class)
class RolePermissionEvaluatorEquivalenciaTest {

    private static final Integer USUARIO = 7;
    private static final Integer ROL = 20;
    private static final Integer EMPRESA = 3;
    private static final List<String> ACCIONES =
            List.of("LEER", "ESCRIBIR", "CREAR", "MODIFICAR", "EDITAR", "ELIMINAR", "BORRAR", "");

    @Mock private RolVistaPermisoRepository rolVistaPermisoRepository;
    @Mock private UsuarioPorRolRepository usuarioPorRolRepository;

    private RolePermissionEvaluator evaluador;
    private Role rol;

    @BeforeEach
    void preparar() {
        evaluador = new RolePermissionEvaluator(new PermisosDelRol(rolVistaPermisoRepository), usuarioPorRolRepository);
        rol = new Role();
        rol.setId(ROL);
        rol.setScope(RoleScope.STAFF);
        rol.setPurpose(RolePurpose.CUSTOM);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setRol(rol);
        lenient().when(usuarioPorRolRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(USUARIO, ROL, EMPRESA))
                .thenReturn(Optional.of(asignacion));
    }

    @AfterEach
    void cerrarLaSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void unPermisoDeLecturaYEscrituraDaLasMismasRespuestasEnUnaSolicitudQueFuera() {
        RolVistaPermiso permiso = permiso("VISTA_MASCOTAS", true, ViewAudience.STAFF, true, true, false, false, DataScope.COMPANY);

        assertMismasDecisiones(permiso, "VISTA_MASCOTAS");
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_MASCOTAS", "ESCRIBIR"))).isTrue();
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_MASCOTAS", "ELIMINAR"))).isFalse();
    }

    @Test
    void unaVistaInactivaSeRechazaDeLaMismaFormaEnAmbosModos() {
        RolVistaPermiso permiso = permiso("VISTA_PAGOS", false, ViewAudience.STAFF, true, true, true, true, DataScope.COMPANY);

        assertMismasDecisiones(permiso, "VISTA_PAGOS");
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_PAGOS", "LEER"))).isFalse();
    }

    @Test
    void unaVistaDeOtraAudienciaSeRechazaDeLaMismaFormaEnAmbosModos() {
        RolVistaPermiso permiso = permiso("VISTA_MIS_MASCOTAS", true, ViewAudience.CLIENT, true, true, true, true, DataScope.OWN);

        assertMismasDecisiones(permiso, "VISTA_MIS_MASCOTAS");
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_MIS_MASCOTAS", "LEER"))).isFalse();
    }

    @Test
    void unaVistaCompartidaFuncionaParaCualquierAudienciaEnAmbosModos() {
        RolVistaPermiso permiso = permiso("VISTA_PERFIL", true, ViewAudience.SHARED, true, false, false, false, DataScope.OWN);

        assertMismasDecisiones(permiso, "VISTA_PERFIL");
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_PERFIL", "LEER"))).isTrue();
    }

    @Test
    void elAlcanceDeLosDatosEsElMismoYSoloAmpliaConLectura() {
        RolVistaPermiso conLectura = permiso("VISTA_CITAS_AGENDA", true, ViewAudience.STAFF, true, false, false, false, DataScope.COMPANY);
        assertMismasDecisiones(conLectura, "VISTA_CITAS_AGENDA");
        assertThat(enUnaSolicitud(() -> evaluador.dataScope(USUARIO, ROL, EMPRESA, "VISTA_CITAS_AGENDA")))
                .isEqualTo(DataScope.COMPANY);

        RolVistaPermiso sinLectura = permiso("VISTA_CITAS_AGENDA", true, ViewAudience.STAFF, false, false, false, false, DataScope.COMPANY);
        assertMismasDecisiones(sinLectura, "VISTA_CITAS_AGENDA");
        assertThat(enUnaSolicitud(() -> evaluador.dataScope(USUARIO, ROL, EMPRESA, "VISTA_CITAS_AGENDA")))
                .isEqualTo(DataScope.OWN);
    }

    @Test
    void unaVistaSinPermisoParaElRolSeRechazaEnAmbosModos() {
        stubPermisos(List.of(permiso("VISTA_MASCOTAS", true, ViewAudience.STAFF, true, true, true, true, DataScope.COMPANY)));

        assertThat(fueraDeUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_OTRA", "LEER"))).isFalse();
        assertThat(enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_OTRA", "LEER"))).isFalse();
    }

    private void assertMismasDecisiones(RolVistaPermiso permiso, String codigoDeVista) {
        stubPermisos(List.of(permiso));
        for (String accion : ACCIONES) {
            boolean fuera = fueraDeUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, codigoDeVista, accion));
            boolean dentro = enUnaSolicitud(() -> evaluador.can(USUARIO, ROL, EMPRESA, codigoDeVista, accion));
            boolean trasOtraVista = trasConsultarOtraVista(() -> evaluador.can(USUARIO, ROL, EMPRESA, codigoDeVista, accion));
            assertThat(dentro).as("acción %s de %s", accion, codigoDeVista).isEqualTo(fuera);
            assertThat(trasOtraVista).as("acción %s de %s tras otra vista", accion, codigoDeVista).isEqualTo(fuera);
        }
        DataScope alcanceFuera = fueraDeUnaSolicitud(() -> evaluador.dataScope(USUARIO, ROL, EMPRESA, codigoDeVista));
        DataScope alcanceDentro = enUnaSolicitud(() -> evaluador.dataScope(USUARIO, ROL, EMPRESA, codigoDeVista));
        DataScope alcanceTrasOtraVista = trasConsultarOtraVista(() -> evaluador.dataScope(USUARIO, ROL, EMPRESA, codigoDeVista));
        assertThat(alcanceDentro).as("alcance de %s", codigoDeVista).isEqualTo(alcanceFuera);
        assertThat(alcanceTrasOtraVista).as("alcance de %s tras otra vista", codigoDeVista).isEqualTo(alcanceFuera);
    }

    private <T> T trasConsultarOtraVista(java.util.function.Supplier<T> accion) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        evaluador.can(USUARIO, ROL, EMPRESA, "VISTA_CONSULTADA_ANTES", "LEER");
        return accion.get();
    }

    private void stubPermisos(List<RolVistaPermiso> permisos) {
        lenient().when(rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(ROL)).thenReturn(permisos);
        for (RolVistaPermiso permiso : permisos) {
            lenient().when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, permiso.getVista().getCodigo()))
                    .thenReturn(Optional.of(permiso));
        }
    }

    private <T> T fueraDeUnaSolicitud(java.util.function.Supplier<T> accion) {
        RequestContextHolder.resetRequestAttributes();
        return accion.get();
    }

    private <T> T enUnaSolicitud(java.util.function.Supplier<T> accion) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        return accion.get();
    }

    private RolVistaPermiso permiso(String codigoDeVista, boolean vistaActiva, ViewAudience audiencia,
                                    boolean leer, boolean escribir, boolean modificar, boolean eliminar, DataScope alcance) {
        Vista vista = new Vista();
        vista.setCodigo(codigoDeVista);
        vista.setActivo(vistaActiva);
        vista.setAudience(audiencia);
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setRol(rol);
        permiso.setVista(vista);
        permiso.setLeer(leer);
        permiso.setEscribir(escribir);
        permiso.setModificar(modificar);
        permiso.setEliminar(eliminar);
        permiso.setDataScope(alcance);
        return permiso;
    }
}
