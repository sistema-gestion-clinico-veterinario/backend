package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataMaskingTest {

    @Test
    void elDocumentoMuestraSoloLosUltimosCuatroCaracteres() {
        assertThat(DataMasking.documento("12345678")).isEqualTo("****5678");
        assertThat(DataMasking.documento("A12345678")).isEqualTo("*****5678");
        assertThat(DataMasking.documento(" 123456789 ")).isEqualTo("*****6789");
    }

    @Test
    void unDocumentoCortoSeOcultaPorCompleto() {
        assertThat(DataMasking.documento("1234")).isEqualTo("****");
        assertThat(DataMasking.documento("12")).isEqualTo("**");
    }

    @Test
    void elTelefonoMuestraSoloLosUltimosTresDigitosYConservaLosSeparadores() {
        assertThat(DataMasking.telefono("987654321")).isEqualTo("******321");
        assertThat(DataMasking.telefono("+51 987 654 321")).isEqualTo("+** *** *** 321");
        assertThat(DataMasking.telefono("987-654-321")).isEqualTo("***-***-321");
    }

    @Test
    void unTelefonoMuyCortoSeOcultaPorCompleto() {
        assertThat(DataMasking.telefono("123")).isEqualTo("***");
    }

    @Test
    void elCorreoMuestraLaPrimeraLetraYElDominio() {
        assertThat(DataMasking.correo("ana.paredes@clinica.pe")).isEqualTo("a***@clinica.pe");
        assertThat(DataMasking.correo("x@gmail.com")).isEqualTo("x***@gmail.com");
        assertThat(DataMasking.correo("sinarroba")).isEqualTo("s***");
    }

    @Test
    void losValoresVaciosNoSeAlteran() {
        assertThat(DataMasking.documento(null)).isNull();
        assertThat(DataMasking.telefono("  ")).isEqualTo("  ");
        assertThat(DataMasking.correo("")).isEqualTo("");
    }

    @Test
    void elValorOcultoNuncaContieneElOriginal() {
        String documento = "45871236";
        String telefono = "987654321";
        String correo = "maria.lopez@clinica.pe";

        assertThat(DataMasking.documento(documento)).doesNotContain("45871");
        assertThat(DataMasking.telefono(telefono)).doesNotContain("98765");
        assertThat(DataMasking.correo(correo)).doesNotContain("maria.lopez");
    }
}
