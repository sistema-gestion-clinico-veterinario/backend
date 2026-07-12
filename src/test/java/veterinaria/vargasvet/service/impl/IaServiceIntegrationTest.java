package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;
import veterinaria.vargasvet.integration.LaboratorioIAClient;
import veterinaria.vargasvet.integration.RadiografiaIAClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IaServiceIntegrationTest {

    private MockRestServiceServer server;
    private LaboratorioIAServiceImpl laboratorioService;
    private RadiografiaIAServiceImpl radiografiaService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);

        LaboratorioIAClient laboratorioClient = new LaboratorioIAClient(restTemplate);
        ReflectionTestUtils.setField(laboratorioClient, "iaUrl", "http://ia.test");
        laboratorioService = new LaboratorioIAServiceImpl(laboratorioClient);

        RadiografiaIAClient radiografiaClient = new RadiografiaIAClient(restTemplate);
        ReflectionTestUtils.setField(radiografiaClient, "iaUrl", "http://ia.test");
        radiografiaService = new RadiografiaIAServiceImpl(radiografiaClient);
    }

    @Test
    @DisplayName("[BB-013] Analizar laboratorio valido muestra la interpretacion de IA")
    void laboratorioIaEnviaArchivoYMapeaRespuestaExterna() {
        server.expect(requestTo("http://ia.test/ia/laboratorio"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "fuente": "mock",
                          "tipo": "laboratorio",
                          "especie": "PERRO",
                          "alertas": ["Leucocitos altos"],
                          "secciones_presentes": ["hematologia"],
                          "comentarios_clinicos": ["Correlacionar con signos clinicos"]
                        }
                        """, MediaType.APPLICATION_JSON));

        LaboratorioIAResponse response = laboratorioService.analizarLaboratorio(
                new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "contenido".getBytes()),
                "PERRO"
        );

        assertThat(response.getTipo()).isEqualTo("laboratorio");
        assertThat(response.getEspecie()).isEqualTo("PERRO");
        assertThat(response.getAlertas()).contains("Leucocitos altos");
        server.verify();
    }

    @Test
    void laboratorioIaPropagaErrorControladoSiServicioExternoFalla() {
        server.expect(requestTo("http://ia.test/ia/laboratorio"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> laboratorioService.analizarLaboratorio(
                new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "contenido".getBytes()),
                "PERRO"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al conectar con el servicio de an");

        server.verify();
    }

    @Test
    void radiografiaIaEnviaImagenYMapeaPrediccionExterna() {
        server.expect(requestTo("http://ia.test/predict/radiografia"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "model": "rx-vet-test",
                          "file_type": "jpg",
                          "diagnoses": ["Sin hallazgos oseos agudos"],
                          "inference_ms": 42.5,
                          "predictions": {}
                        }
                        """, MediaType.APPLICATION_JSON));

        RadiografiaPrediccionResponse response = radiografiaService.analizarRadiografia(
                new MockMultipartFile("file", "torax.jpg", "image/jpeg", "imagen".getBytes())
        );

        assertThat(response.getModel()).isEqualTo("rx-vet-test");
        assertThat(response.getFileType()).isEqualTo("jpg");
        assertThat(response.getDiagnoses()).contains("Sin hallazgos oseos agudos");
        assertThat(response.getInferenceMs()).isEqualTo(42.5);
        server.verify();
    }
}
