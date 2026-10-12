package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import veterinaria.vargasvet.controller.ApoderadoPortalController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cancelar una cita cambia su estado; eliminar la borra. Son operaciones distintas y no comparten verbo ni permiso.
 */
class CancelarNoEsEliminarTest {

    @Test
    void ningunaOperacionDeCancelarUsaElVerboDelete() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> infractores = new ArrayList<>();
        int cancelaciones = 0;

        for (BeanDefinition definicion : escaner.findCandidateComponents("veterinaria.vargasvet.controller")) {
            Class<?> controlador = Class.forName(definicion.getBeanClassName());
            for (Method metodo : controlador.getDeclaredMethods()) {
                String[] rutasDelete = metodo.isAnnotationPresent(DeleteMapping.class)
                        ? metodo.getAnnotation(DeleteMapping.class).value() : new String[0];
                String[] rutasPatch = metodo.isAnnotationPresent(PatchMapping.class)
                        ? metodo.getAnnotation(PatchMapping.class).value() : new String[0];
                if (Arrays.stream(rutasDelete).anyMatch(ruta -> ruta.endsWith("/cancel"))) {
                    infractores.add(controlador.getSimpleName() + "." + metodo.getName());
                }
                if (Arrays.stream(rutasPatch).anyMatch(ruta -> ruta.endsWith("/cancel"))) {
                    cancelaciones++;
                }
            }
        }

        assertThat(cancelaciones).as("la prueba tiene dientes: hay cancelaciones por PATCH").isGreaterThanOrEqualTo(3);
        assertThat(infractores).as("Cancelaciones expuestas como DELETE").isEmpty();
    }

    @Test
    void elClienteCancelaSuCitaConElPermisoDeModificarNoConElDeEliminar() throws Exception {
        Method cancelar = Arrays.stream(ApoderadoPortalController.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().equals("cancelPortalCita")).findFirst().orElseThrow();

        String regla = cancelar.getAnnotation(PreAuthorize.class).value();

        assertThat(regla).contains("VISTA_MIS_CITAS").contains("MODIFICAR").doesNotContain("ELIMINAR");
        assertThat(cancelar.isAnnotationPresent(DeleteMapping.class)).isFalse();
        assertThat(cancelar.isAnnotationPresent(PatchMapping.class)).isTrue();
    }
}
