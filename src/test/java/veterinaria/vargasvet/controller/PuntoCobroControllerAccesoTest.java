package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.request.PuntoCobroRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica que la configuración de puntos de cobro sea exclusiva de administradores. */
class PuntoCobroControllerAccesoTest {

    private String regla(String metodo, Class<?>... parametros) throws NoSuchMethodException {
        Method controllerMethod = PuntoCobroController.class.getMethod(metodo, parametros);
        PreAuthorize authorization = controllerMethod.getAnnotation(PreAuthorize.class);
        assertThat(authorization)
                .as("el método %s debe declarar su regla de acceso", metodo)
                .isNotNull();
        return authorization.value();
    }

    @Test
    void listarYAdministrarPuntosExigeUnRolAdministrador() throws Exception {
        assertSoloAdministradores(regla("listar", Integer.class));
        assertSoloAdministradores(regla("crear", PuntoCobroRequest.class));
        assertSoloAdministradores(regla("actualizar", Long.class, PuntoCobroRequest.class));
        assertSoloAdministradores(regla("vincular", Long.class, Integer.class,
                HttpServletRequest.class, HttpServletResponse.class));
        assertSoloAdministradores(regla("desvincular", Long.class, Integer.class,
                HttpServletRequest.class, HttpServletResponse.class));
    }

    @Test
    void consultarElEstadoDelEquipoSigueDisponibleAlPersonalDeCaja() throws Exception {
        assertThat(regla("esteEquipo", Integer.class))
                .contains("VISTA_CAJA")
                .contains("LEER");
    }

    private void assertSoloAdministradores(String regla) {
        assertThat(regla)
                .contains("hasPurpose('COMPANY_ADMIN')")
                .contains("hasPurpose('PLATFORM_ADMIN')")
                .doesNotContain("VISTA_CAJA");
    }
}
