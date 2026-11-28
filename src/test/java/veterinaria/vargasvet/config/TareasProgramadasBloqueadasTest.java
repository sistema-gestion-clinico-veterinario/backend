package veterinaria.vargasvet.config;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TareasProgramadasBloqueadasTest {

    private static final Set<String> IDEMPOTENTES_O_LOCALES = Set.of(
            "veterinaria.vargasvet.security.AccountLockoutService#deleteStaleCounters",
            "veterinaria.vargasvet.security.SharedRateLimitService#deleteExpiredCounters",
            "veterinaria.vargasvet.service.RealtimeTicketService#deleteExpiredTickets",
            "veterinaria.vargasvet.security.RealtimeSubscriptionGuard#revalidate");

    private static List<Method> tareasProgramadas() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        List<Method> metodos = new ArrayList<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents("veterinaria.vargasvet")) {
            Class<?> clase = Class.forName(definicion.getBeanClassName());
            for (Method metodo : clase.getDeclaredMethods()) {
                if (metodo.isAnnotationPresent(Scheduled.class)) {
                    metodos.add(metodo);
                }
            }
        }
        return metodos;
    }

    private static String clave(Method metodo) {
        return metodo.getDeclaringClass().getName() + "#" + metodo.getName();
    }

    @Test
    void todaTareaProgramadaConEfectoTomaUnBloqueoYSoloLasIdempotentesQuedanSinEl() throws Exception {
        List<Method> tareas = tareasProgramadas();
        assertThat(tareas).hasSizeGreaterThanOrEqualTo(8);

        for (Method tarea : tareas) {
            boolean bloqueada = tarea.isAnnotationPresent(SchedulerLock.class);
            boolean exenta = IDEMPOTENTES_O_LOCALES.contains(clave(tarea));
            assertThat(bloqueada || exenta)
                    .as("%s es @Scheduled: con varias instancias correría en todas; debe llevar @SchedulerLock o estar en la lista de exentas", clave(tarea))
                    .isTrue();
            assertThat(bloqueada && exenta).as("%s está bloqueada y también exenta", clave(tarea)).isFalse();
        }
    }

    @Test
    void lasExencionesSiguenExistiendo() throws Exception {
        Set<String> existentes = new HashSet<>();
        for (Method tarea : tareasProgramadas()) {
            existentes.add(clave(tarea));
        }

        assertThat(existentes).containsAll(IDEMPOTENTES_O_LOCALES);
    }

    @Test
    void losBloqueosTienenNombreUnicoYTiemposCoherentes() throws Exception {
        Set<String> nombres = new HashSet<>();
        int bloqueadas = 0;
        for (Method tarea : tareasProgramadas()) {
            SchedulerLock bloqueo = tarea.getAnnotation(SchedulerLock.class);
            if (bloqueo == null) {
                continue;
            }
            bloqueadas++;
            assertThat(nombres.add(bloqueo.name())).as("nombre repetido: %s", bloqueo.name()).isTrue();
            assertThat(bloqueo.name().length()).isLessThanOrEqualTo(64);
            Duration maximo = Duration.parse(bloqueo.lockAtMostFor());
            Duration minimo = Duration.parse(bloqueo.lockAtLeastFor());
            assertThat(minimo).as(bloqueo.name()).isPositive().isLessThan(maximo);
            assertThat(maximo).as(bloqueo.name()).isLessThanOrEqualTo(Duration.ofHours(2));
        }

        assertThat(bloqueadas).isEqualTo(4);
        assertThat(nombres).containsExactlyInAnyOrder(
                "recordatorios-preventivos", "purga-cuentas-cerradas",
                "revision-autorizacion-mascotas", "cierre-vinculos-vencidos");
    }
}
