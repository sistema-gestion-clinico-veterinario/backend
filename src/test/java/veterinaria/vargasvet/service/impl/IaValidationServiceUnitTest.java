package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;
import veterinaria.vargasvet.integration.LaboratorioIAClient;
import veterinaria.vargasvet.integration.RadiografiaIAClient;
import veterinaria.vargasvet.security.AiServiceScope;
import veterinaria.vargasvet.security.AiServiceTokenProvider;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IaValidationServiceUnitTest {

    @Mock
    private LaboratorioIAClient laboratorioIAClient;

    @Mock
    private RadiografiaIAClient radiografiaIAClient;

    @Mock
    private AiServiceTokenProvider aiServiceTokenProvider;

    @Test
    void analizarLaboratorio_rechazaArchivoVacio() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarLaboratorio(file, "PERRO", 1L)
        );

        assertEquals("El archivo no puede estar vacío.", ex.getMessage());
        verify(laboratorioIAClient, never()).analizar(file, "PERRO", "token");
    }

    @Test
    void analizarLaboratorio_rechazaExtensionNoSoportada() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.exe", "application/octet-stream", "x".getBytes());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarLaboratorio(file, "GATO", 1L)
        );

        assertEquals("Formato no soportado: \".exe\". Use PDF, JPG, PNG, BMP o TIFF.", ex.getMessage());
        verify(laboratorioIAClient, never()).analizar(file, "GATO", "token");
    }

    @Test
    void analizarLaboratorio_retornaRespuestaDelClienteCuandoArchivoEsValido() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "pdf".getBytes());
        LaboratorioIAResponse expected = new LaboratorioIAResponse();
        expected.setEspecie("PERRO");
        when(aiServiceTokenProvider.issue(AiServiceScope.LABORATORIO, 1L)).thenReturn("token");
        when(laboratorioIAClient.analizar(file, "PERRO", "token")).thenReturn(expected);

        LaboratorioIAResponse response = service.analizarLaboratorio(file, "PERRO", 1L);

        assertSame(expected, response);
    }

    @Test
    void analizarLaboratorio_envuelveErrorDelCliente() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "pdf".getBytes());
        when(aiServiceTokenProvider.issue(AiServiceScope.LABORATORIO, 1L)).thenReturn("token");
        when(laboratorioIAClient.analizar(file, "PERRO", "token")).thenThrow(new IOException("timeout"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.analizarLaboratorio(file, "PERRO", 1L)
        );

        assertEquals("Error al conectar con el servicio de análisis de laboratorio: timeout", ex.getMessage());
    }

    @Test
    void analizarRadiografia_rechazaArchivoVacio() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("file", "rx.dcm", "application/dicom", new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarRadiografia(file, 1L)
        );

        assertEquals("El archivo de radiografía no puede estar vacío.", ex.getMessage());
        verify(radiografiaIAClient, never()).predict(file, "token");
    }

    @Test
    @DisplayName("[BB-014] Analizar radiografia con formato invalido es rechazado")
    void analizarRadiografia_rechazaFormatoNoSoportado() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("file", "rx.pdf", "application/pdf", "pdf".getBytes());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarRadiografia(file, 1L)
        );

        assertEquals("Formato no soportado. Use DICOM (.dcm), PNG o JPG.", ex.getMessage());
        verify(radiografiaIAClient, never()).predict(file, "token");
    }

    @Test
    void analizarRadiografia_retornaRespuestaDelClienteCuandoArchivoEsValido() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient, aiServiceTokenProvider);
        MultipartFile file = new MockMultipartFile("file", "rx.dcm", "application/dicom", "dicom".getBytes());
        RadiografiaPrediccionResponse expected = new RadiografiaPrediccionResponse();
        expected.setModel("rx-model");
        when(aiServiceTokenProvider.issue(AiServiceScope.RADIOGRAFIA, 1L)).thenReturn("token");
        when(radiografiaIAClient.predict(file, "token")).thenReturn(expected);

        RadiografiaPrediccionResponse response = service.analizarRadiografia(file, 1L);

        assertSame(expected, response);
    }
}
