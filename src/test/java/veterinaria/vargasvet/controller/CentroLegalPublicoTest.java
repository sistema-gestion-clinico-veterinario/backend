package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.dto.response.AvisoPublicoResponse;
import veterinaria.vargasvet.dto.response.CentroLegalPublicoResponse;
import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.service.AvisoPrivacidadService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CentroLegalPublicoTest {

    @Test
    void entregaElAvisoDeLaClinicaYLosDocumentosDeSoftVetEnUnaSolaRespuesta() {
        AvisoPrivacidadService avisos = mock(AvisoPrivacidadService.class);
        LegalDocumentService legales = mock(LegalDocumentService.class);
        PrivacidadController controller = new PrivacidadController(avisos,
                mock(ConsentimientoDatosService.class), mock(ClientIpResolver.class), legales);
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 19, 10, 0);
        when(avisos.publico("patitas", AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS))
                .thenReturn(new AvisoPublicoResponse("Clínica Patitas", "logo.webp", "#0891b2",
                        AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS, 3, ahora, "Aviso"));
        when(legales.getActiveDocuments()).thenReturn(List.of(
                new LegalDocumentDTO(10L, LegalDocumentType.TERMINOS_Y_CONDICIONES,
                        "2.0", "Términos", ahora),
                new LegalDocumentDTO(11L, LegalDocumentType.POLITICA_PRIVACIDAD,
                        "1.1", "Política", ahora)));

        CentroLegalPublicoResponse centro = controller.centroLegalPublico("patitas",
                AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS, null).getBody().getData();

        assertThat(centro.clinica()).isEqualTo("Clínica Patitas");
        assertThat(centro.documentos()).extracting("codigo")
                .containsExactly("AVISO_PRIVACIDAD", "TERMINOS_Y_CONDICIONES", "POLITICA_PRIVACIDAD");
        assertThat(centro.documentos()).extracting("titulo")
                .containsExactly("Aviso de privacidad", "Términos y condiciones de SoftVet",
                        "Política de privacidad de SoftVet");
        verify(avisos).publico("patitas", AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
        verify(legales).getActiveDocuments();
    }

    @Test
    void conservaLaConsultaDeUnaVersionHistoricaDelAviso() {
        AvisoPrivacidadService avisos = mock(AvisoPrivacidadService.class);
        LegalDocumentService legales = mock(LegalDocumentService.class);
        PrivacidadController controller = new PrivacidadController(avisos,
                mock(ConsentimientoDatosService.class), mock(ClientIpResolver.class), legales);
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 1, 8, 0);
        when(avisos.publico("patitas", AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS, 1))
                .thenReturn(new AvisoPublicoResponse("Clínica Patitas", null, null,
                        AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS, 1, fecha, "Aviso anterior"));
        when(legales.getActiveDocuments()).thenReturn(List.of());

        CentroLegalPublicoResponse centro = controller.centroLegalPublico("patitas",
                AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS, 1).getBody().getData();

        assertThat(centro.documentos()).singleElement().satisfies(documento -> {
            assertThat(documento.version()).isEqualTo("1");
            assertThat(documento.titulo()).isEqualTo("Aviso de privacidad para trabajadores");
            assertThat(documento.contenido()).isEqualTo("Aviso anterior");
        });
        verify(avisos).publico("patitas", AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS, 1);
    }
}
