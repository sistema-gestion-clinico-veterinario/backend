package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvisoPrivacidadTextoTest {

    private CamposAvisoPrivacidad campos() {
        return new CamposAvisoPrivacidad("Clínica Patitas S.A.C.", "20123456789", "Av. Los Olivos 123, Lima",
                "privacidad@patitas.test", "RNPDP-1234", "VetSoft, proveedor de la plataforma",
                List.of("Atender a su mascota", "Emitir comprobantes"),
                List.of("Nombres y apellidos", "Correo electrónico"), List.of("Observaciones"),
                List.of("El personal de la clínica"), "No se transfieren datos fuera del Perú",
                "Mientras sea cliente y 10 años después");
    }

    @Test
    void elTextoIncluyeTodoLoQueExigeElDeberDeInformar() {
        String texto = AvisoPrivacidadTexto.componer(campos(), 3);

        assertThat(texto)
                .contains("Versión 3")
                .contains("Clínica Patitas S.A.C.").contains("20123456789").contains("Av. Los Olivos 123, Lima")
                .contains("RNPDP-1234").contains("VetSoft, proveedor de la plataforma")
                .contains("Nombres y apellidos").contains("Observaciones")
                .contains("Atender a su mascota").contains("El personal de la clínica")
                .contains("No se transfieren datos fuera del Perú")
                .contains("Consecuencias de proporcionar sus datos o de negarse")
                .contains("Mientras sea cliente y 10 años después")
                .contains("privacidad@patitas.test")
                .contains("solo le enviaremos por correo recordatorios")
                .contains("retirar su autorización para recibir recordatorios por correo")
                .contains("Autoridad Nacional de Protección de Datos Personales");
    }

    @Test
    void elTextoComponenteNuncaSeConfundeConUnBorrador() {
        assertThat(LegalText.draftMarkers(AvisoPrivacidadTexto.componer(campos(), 1))).isEmpty();
    }

    @Test
    void sinDatosFacultativosNiBancoNiEncargadoSeOmitenEsasLineas() {
        CamposAvisoPrivacidad sinOpcionales = campos();
        sinOpcionales.setDatosFacultativos(List.of());
        sinOpcionales.setRegistroBancoDatos(null);
        sinOpcionales.setEncargadoTratamiento(null);

        String texto = AvisoPrivacidadTexto.componer(sinOpcionales, 1);

        assertThat(texto).doesNotContain("Datos facultativos").doesNotContain("Banco de datos inscrito")
                .doesNotContain("Encargado del tratamiento");
    }

    @Test
    void limpiarQuitaEspaciosRepetidosVaciosYDuplicados() {
        CamposAvisoPrivacidad sucio = campos();
        sucio.setRazonSocial("  Clínica   Patitas  ");
        sucio.setRegistroBancoDatos("   ");
        sucio.setFinalidades(Arrays.asList(" Atender ", "Atender", "", null, "Cobrar"));

        CamposAvisoPrivacidad limpio = AvisoPrivacidadTexto.limpiar(sucio);

        assertThat(limpio.getRazonSocial()).isEqualTo("Clínica Patitas");
        assertThat(limpio.getRegistroBancoDatos()).isNull();
        assertThat(limpio.getFinalidades()).containsExactly("Atender", "Cobrar");
    }
}
