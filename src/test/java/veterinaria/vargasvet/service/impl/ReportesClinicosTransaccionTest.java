package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportesClinicosTransaccionTest {

    private static final List<String> CONSULTAS = List.of(
            "obtenerReportes", "obtenerComparativoEmpresas", "obtenerPacientesInactivos", "obtenerVentasProductos");

    @Test
    void cadaReporteComparteUnaSolaSesionDeSoloLectura() {
        for (String nombre : CONSULTAS) {
            Method metodo = Arrays.stream(ReportesClinicosServiceImpl.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nombre))
                    .findFirst()
                    .orElseThrow();
            Transactional transaccional = metodo.getAnnotation(Transactional.class);
            assertThat(transaccional).as(nombre).isNotNull();
            assertThat(transaccional.readOnly()).as(nombre).isTrue();
        }
    }
}
