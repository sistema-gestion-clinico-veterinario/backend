package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegalTextTest {

    @Test
    void laHuellaEsUnSha256EnHexadecimalYEstable() {
        assertThat(LegalText.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(LegalText.sha256Hex("texto")).isEqualTo(LegalText.sha256Hex("texto")).hasSize(64);
    }

    @Test
    void laHuellaDependeDeCadaCaracter_incluidasLasTildes() {
        assertThat(LegalText.sha256Hex("Política")).isNotEqualTo(LegalText.sha256Hex("Politica"));
    }

    @Test
    void normalizarUnificaSaltosDeLineaYQuitaLosEspaciosDeLosExtremos() {
        assertThat(LegalText.normalize("  uno\r\ndos\rtres \n ")).isEqualTo("uno\ndos\ntres");
        assertThat(LegalText.normalize(null)).isEmpty();
        assertThat(LegalText.sha256Hex(LegalText.normalize("a\r\nb"))).isEqualTo(LegalText.sha256Hex(LegalText.normalize("a\nb")));
    }

    @Test
    void detectaLosCamposSinCompletarYLasFrasesDeBorrador() {
        assertThat(LegalText.draftMarkers("Titular: [Razón Social], plazo [POR COMPLETAR] días. Versión 1.0 — BORRADOR PROVISIONAL"))
                .containsExactlyInAnyOrder("[Razón Social]", "[POR COMPLETAR]", "BORRADOR PROVISIONAL");
        assertThat(LegalText.draftMarkers("Pendiente de validación legal")).hasSize(1);
        assertThat(LegalText.draftMarkers("Plazo de [pendiente de definir] días")).hasSize(1);
        assertThat(LegalText.draftMarkers("Campo [XXX]")).hasSize(1);
    }

    @Test
    void detectaCualquierCampoEntreCorchetesYLosMarcadoresDePlantilla() {
        for (String texto : List.of(
                "RUC [RUC], domicilio [Dirección]", "Contacto: [correo de contacto]", "RUC: XXXXXXXXXXX", "RUC: 00000000000",
                "Razón social: ________", "Titular <RAZON SOCIAL>", "{{razon_social}}", "${empresa}", "Lorem ipsum dolor sit amet")) {
            assertThat(LegalText.draftMarkers(texto)).as(texto).isNotEmpty();
        }
    }

    @Test
    void laPalabraBorradorOTodoUsadasConSentidoNoSonMarcas() {
        assertThat(LegalText.draftMarkers("El borrador de la receta se guarda en el historial de la consulta.")).isEmpty();
        assertThat(LegalText.draftMarkers("Para todo uso de la plataforma, todo cliente acepta estos términos.")).isEmpty();
        assertThat(LegalText.draftMarkers("Se atiende en <slug>.vetsoft.pe y el RUC es 20123456789.")).isEmpty();
    }

    @Test
    void unTextoCompletoNoTieneMarcas_ySeAceptanLosCorchetesLegitimos() {
        assertThat(LegalText.draftMarkers("Conforme al artículo 5 [1] de la Ley N.° 29733, el titular decide.")).isEmpty();
        assertThat(LegalText.draftMarkers(null)).isEmpty();
    }
}
