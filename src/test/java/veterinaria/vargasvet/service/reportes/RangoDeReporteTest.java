package veterinaria.vargasvet.service.reportes;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangoDeReporteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

    @Test
    void sinFechasUsaDesdeElPrimerDiaDelMesHastaHoy() {
        RangoDeReporte rango = RangoDeReporte.validado(null, null, HOY);

        assertThat(rango.desde()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(rango.hasta()).isEqualTo(HOY);
    }

    @Test
    void respetaLasFechasIndicadas() {
        RangoDeReporte rango = RangoDeReporte.validado(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 10), HOY);

        assertThat(rango.desde()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(rango.hasta()).isEqualTo(LocalDate.of(2026, 2, 10));
    }

    @Test
    void rechazaUnaFechaHastaAnteriorALaFechaDesde() {
        assertThatThrownBy(() -> RangoDeReporte.validado(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1), HOY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha hasta no puede ser anterior a la fecha desde");
    }

    @Test
    void aceptaUnRangoDeExactamente1110Dias() {
        LocalDate desde = LocalDate.of(2023, 1, 1);

        RangoDeReporte rango = RangoDeReporte.validado(desde, desde.plusDays(1110), HOY);

        assertThat(rango.hasta()).isEqualTo(desde.plusDays(1110));
    }

    @Test
    void rechazaUnRangoMayorA1110Dias() {
        LocalDate desde = LocalDate.of(2023, 1, 1);

        assertThatThrownBy(() -> RangoDeReporte.validado(desde, desde.plusDays(1111), HOY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El rango máximo permitido para reportes es de 36 meses");
    }

    @Test
    void cuentaLosDiasIncluyendoAmbosExtremos() {
        assertThat(new RangoDeReporte(HOY, HOY).dias()).isEqualTo(1);
        assertThat(new RangoDeReporte(HOY.minusDays(29), HOY).dias()).isEqualTo(30);
    }

    @Test
    void elPeriodoAnteriorTerminaElDiaPrevioYDuraLoMismo() {
        RangoDeReporte rango = new RangoDeReporte(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));

        RangoDeReporte anterior = rango.anterior();

        assertThat(anterior.hasta()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(anterior.desde()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(anterior.dias()).isEqualTo(rango.dias());
    }

    @Test
    void elPeriodoAnteriorDeUnSoloDiaEsElDiaPrevio() {
        RangoDeReporte anterior = new RangoDeReporte(HOY, HOY).anterior();

        assertThat(anterior.desde()).isEqualTo(HOY.minusDays(1));
        assertThat(anterior.hasta()).isEqualTo(HOY.minusDays(1));
    }

    @Test
    void elFinEsExclusivoYEmpiezaAlInicioDelDiaSiguiente() {
        RangoDeReporte rango = new RangoDeReporte(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));

        assertThat(rango.inicio()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(rango.finExclusivo()).isEqualTo(LocalDateTime.of(2026, 11, 11, 0, 0));
    }

    @Test
    void elFinExclusivoDelPeriodoAnteriorEsElInicioDelPeriodoActual() {
        RangoDeReporte rango = new RangoDeReporte(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10));

        assertThat(rango.anterior().finExclusivo()).isEqualTo(rango.inicio());
    }
}
