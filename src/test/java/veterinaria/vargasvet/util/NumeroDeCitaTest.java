package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NumeroDeCitaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 23);

    @Test
    void tieneLaFechaYUnSufijoAleatorioDeDiezCaracteresQueCabeEnLaColumna() {
        String numero = NumeroDeCita.generar(DIA);

        assertThat(numero).matches("CIT-20261123-[0-9A-F]{10}");
        assertThat(numero.length()).isLessThanOrEqualTo(NumeroDeCita.LONGITUD_MAXIMA);
    }

    @Test
    void diezMilNumerosDelMismoDiaNoSeRepiten() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            vistos.add(NumeroDeCita.generar(DIA));
        }

        assertThat(vistos).hasSize(10_000);
    }
}
