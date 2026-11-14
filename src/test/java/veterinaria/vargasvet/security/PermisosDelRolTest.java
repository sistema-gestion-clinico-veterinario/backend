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
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermisosDelRolTest {

    private static final Integer ROL = 20;

    @Mock private RolVistaPermisoRepository rolVistaPermisoRepository;

    private PermisosDelRol permisos;

    @BeforeEach
    void preparar() {
        permisos = new PermisosDelRol(rolVistaPermisoRepository);
    }

    @AfterEach
    void cerrarLaSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void unaSolaVistaPorSolicitudConsultaSoloEsaVistaYNoCargaElRolCompleto() {
        abrirSolicitud();
        RolVistaPermiso mascotas = permiso("VISTA_MASCOTAS");
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_MASCOTAS")).thenReturn(Optional.of(mascotas));

        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).containsSame(mascotas);
        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).containsSame(mascotas);

        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(ROL, "VISTA_MASCOTAS");
        verify(rolVistaPermisoRepository, never()).findByRolIdWithVistaAndVentana(any());
    }

    @Test
    void desdeLaSegundaVistaDistintaCargaElRolCompletoUnaSolaVezSinImportarCuantasVistasSePidan() {
        abrirSolicitud();
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS")).thenReturn(Optional.of(permiso("VISTA_EMPLEADOS")));
        when(rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(ROL)).thenReturn(List.of(
                permiso("VISTA_EMPLEADOS"), permiso("VISTA_MASCOTAS"), permiso("VISTA_PAGOS")));

        assertThat(permisos.de(ROL, "VISTA_EMPLEADOS")).isPresent();
        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).isPresent();
        assertThat(permisos.de(ROL, "VISTA_PAGOS")).isPresent();
        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).isPresent();

        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(any(), any());
        verify(rolVistaPermisoRepository, times(1)).findByRolIdWithVistaAndVentana(ROL);
    }

    @Test
    void despuesDeCargarElRolUnaVistaSinPermisoDevuelveVacioSinVolverALeer() {
        abrirSolicitud();
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS")).thenReturn(Optional.of(permiso("VISTA_EMPLEADOS")));
        when(rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(ROL)).thenReturn(List.of(permiso("VISTA_EMPLEADOS")));

        permisos.de(ROL, "VISTA_EMPLEADOS");
        assertThat(permisos.de(ROL, "VISTA_PAGOS")).isEmpty();
        assertThat(permisos.de(ROL, "VISTA_OTRA")).isEmpty();

        verify(rolVistaPermisoRepository, times(1)).findByRolIdWithVistaAndVentana(ROL);
        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(any(), any());
    }

    @Test
    void unaVistaSinPermisoConsultadaDosVecesSeLeeUnaSolaVez() {
        abrirSolicitud();
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_PAGOS")).thenReturn(Optional.empty());

        assertThat(permisos.de(ROL, "VISTA_PAGOS")).isEmpty();
        assertThat(permisos.de(ROL, "VISTA_PAGOS")).isEmpty();

        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(ROL, "VISTA_PAGOS");
        verify(rolVistaPermisoRepository, never()).findByRolIdWithVistaAndVentana(any());
    }

    @Test
    void rolesDistintosDeLaMismaSolicitudNoSeMezclan() {
        abrirSolicitud();
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS")).thenReturn(Optional.of(permiso("VISTA_EMPLEADOS")));
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(21, "VISTA_EMPLEADOS")).thenReturn(Optional.empty());

        assertThat(permisos.de(ROL, "VISTA_EMPLEADOS")).isPresent();
        assertThat(permisos.de(21, "VISTA_EMPLEADOS")).isEmpty();

        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS");
        verify(rolVistaPermisoRepository, times(1)).findByRolIdAndVistaCodigo(21, "VISTA_EMPLEADOS");
    }

    @Test
    void unaSolicitudNuevaVuelveALeerLosPermisosYVeLosCambios() {
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS"))
                .thenReturn(Optional.of(permiso("VISTA_EMPLEADOS")))
                .thenReturn(Optional.empty());

        abrirSolicitud();
        assertThat(permisos.de(ROL, "VISTA_EMPLEADOS")).isPresent();

        abrirSolicitud();
        assertThat(permisos.de(ROL, "VISTA_EMPLEADOS")).isEmpty();

        verify(rolVistaPermisoRepository, times(2)).findByRolIdAndVistaCodigo(ROL, "VISTA_EMPLEADOS");
    }

    @Test
    void fueraDeUnaSolicitudConsultaSoloLaVistaPedidaCadaVezComoAntes() {
        RolVistaPermiso mascotas = permiso("VISTA_MASCOTAS");
        when(rolVistaPermisoRepository.findByRolIdAndVistaCodigo(ROL, "VISTA_MASCOTAS")).thenReturn(Optional.of(mascotas));

        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).containsSame(mascotas);
        assertThat(permisos.de(ROL, "VISTA_MASCOTAS")).containsSame(mascotas);

        verify(rolVistaPermisoRepository, times(2)).findByRolIdAndVistaCodigo(ROL, "VISTA_MASCOTAS");
        verify(rolVistaPermisoRepository, never()).findByRolIdWithVistaAndVentana(any());
    }

    private void abrirSolicitud() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private RolVistaPermiso permiso(String codigoDeVista) {
        Vista vista = new Vista();
        vista.setCodigo(codigoDeVista);
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setVista(vista);
        return permiso;
    }
}
