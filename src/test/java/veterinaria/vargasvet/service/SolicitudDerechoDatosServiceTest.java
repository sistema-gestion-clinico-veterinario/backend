package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudDerechoDatosServiceTest {

    @Test
    void sumaDiasHabilesSinContarFinDeSemana() {
        LocalDate viernes = LocalDate.of(2026, 10, 16);

        assertThat(SolicitudDerechoDatosService.sumarDiasHabiles(viernes, 1))
                .isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(SolicitudDerechoDatosService.sumarDiasHabiles(viernes, 10))
                .isEqualTo(LocalDate.of(2026, 10, 30));
    }
}
