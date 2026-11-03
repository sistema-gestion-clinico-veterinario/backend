package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento;
import veterinaria.vargasvet.dto.request.ResolverCambioCumplimientoRequest;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyComplianceControllerAccesoTest {

    @Test
    void consultarLaBandejaYElContadorExigePropositoYLecturaDeLaVista() throws Exception {
        assertLectura(regla("bandejaGlobal", Integer.class, ControlCumplimientoClinica.class,
                EstadoSolicitudCambioCumplimiento.class, LocalDate.class, LocalDate.class,
                int.class, int.class));
        assertLectura(regla("solicitudesActivas"));
    }

    @Test
    void revisarOAprobarUnaSolicitudExigePermisoDeModificacion() throws Exception {
        assertModificacion(regla("iniciarRevision", Long.class));
        assertModificacion(regla("resolverSolicitud", Long.class, ResolverCambioCumplimientoRequest.class));
    }

    private String regla(String metodo, Class<?>... parametros) throws NoSuchMethodException {
        Method controllerMethod = CompanyComplianceController.class.getMethod(metodo, parametros);
        PreAuthorize authorization = controllerMethod.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        return authorization.value();
    }

    private void assertLectura(String regla) {
        assertThat(regla)
                .contains("hasPurpose('PLATFORM_ADMIN')")
                .contains("can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'LEER')");
    }

    private void assertModificacion(String regla) {
        assertThat(regla)
                .contains("hasPurpose('PLATFORM_ADMIN')")
                .contains("can('VISTA_SOLICITUDES_CUMPLIMIENTO', 'MODIFICAR')");
    }
}
