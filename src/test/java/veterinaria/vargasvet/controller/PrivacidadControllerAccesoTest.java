package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.request.VistaPreviaAvisoRequest;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** El aviso de privacidad solo lo redacta y publica el administrador de la clínica; el resto del personal no. */
class PrivacidadControllerAccesoTest {

    private String regla(String metodo, Class<?>... parametros) throws NoSuchMethodException {
        Method m = PrivacidadController.class.getMethod(metodo, parametros);
        PreAuthorize anotacion = m.getAnnotation(PreAuthorize.class);
        assertThat(anotacion).as("el método " + metodo + " debe tener una regla de acceso").isNotNull();
        return anotacion.value();
    }

    @Test
    void publicarElAvisoExigeSerAdministradorDeLaClinicaYNoSoloTenerElPermisoDeEmpresa() throws Exception {
        String regla = regla("publicar", PublicarAvisoPrivacidadRequest.class, HttpServletRequest.class);

        assertThat(regla).contains("hasPurpose('COMPANY_ADMIN')").doesNotContain("VISTA_COMPANY");
    }

    @Test
    void redactarlo_la_plantilla_y_la_vista_previa_tambien_son_del_administrador() throws Exception {
        assertThat(regla("plantilla", AudienciaAvisoPrivacidad.class))
                .contains("hasPurpose('COMPANY_ADMIN')").doesNotContain("VISTA_COMPANY");
        assertThat(regla("vistaPrevia", VistaPreviaAvisoRequest.class)).contains("hasPurpose('COMPANY_ADMIN')");
    }

    @Test
    void consultarElHistorialSigueAbiertoAQuienTienePermisoDeLecturaDeLaEmpresa() throws Exception {
        assertThat(regla("historial", AudienciaAvisoPrivacidad.class)).contains("VISTA_COMPANY").contains("LEER");
    }
}
