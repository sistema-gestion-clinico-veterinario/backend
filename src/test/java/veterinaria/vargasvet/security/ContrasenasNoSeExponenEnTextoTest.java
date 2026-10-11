package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Una contraseña que llega en una petición no debe poder acabar en un registro, una traza o una herramienta de
 * monitoreo por imprimir el objeto. Revisa todo DTO de entrada con un campo de contraseña.
 */
class ContrasenasNoSeExponenEnTextoTest {

    private static final Pattern CAMPO_DE_CLAVE = Pattern.compile("(?i).*(password|contrasena|clave).*");
    private static final String SECRETO = "Secreto-De-Prueba-9X!";

    @Test
    void ningunDtoDeEntradaImprimeSuContrasena() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AssignableTypeFilter(Object.class));
        List<String> expuestos = new ArrayList<>();
        int revisados = 0;

        for (BeanDefinition definicion : escaner.findCandidateComponents("veterinaria.vargasvet.dto.request")) {
            Class<?> tipo = Class.forName(definicion.getBeanClassName());
            List<Field> camposDeClave = new ArrayList<>();
            for (Field campo : tipo.getDeclaredFields()) {
                if (campo.getType() == String.class && !Modifier.isStatic(campo.getModifiers())
                        && CAMPO_DE_CLAVE.matcher(campo.getName()).matches()) {
                    camposDeClave.add(campo);
                }
            }
            if (camposDeClave.isEmpty()) {
                continue;
            }
            Object instancia;
            try {
                instancia = tipo.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException ex) {
                expuestos.add(tipo.getSimpleName() + " no se puede revisar: no tiene constructor sin argumentos");
                continue;
            }
            revisados++;
            for (Field campo : camposDeClave) {
                campo.setAccessible(true);
                campo.set(instancia, SECRETO);
            }
            if (instancia.toString().contains(SECRETO)) {
                expuestos.add(tipo.getSimpleName() + ".toString() imprime la contraseña");
            }
        }

        assertThat(revisados).as("la prueba tiene dientes: encontró DTOs con contraseñas").isGreaterThanOrEqualTo(8);
        assertThat(expuestos).as("DTOs que exponen contraseñas").isEmpty();
    }
}
