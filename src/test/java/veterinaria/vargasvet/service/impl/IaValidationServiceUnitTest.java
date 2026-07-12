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

    @Test
    void analizarLaboratorio_rechazaArchivoVacio() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarLaboratorio(file, "PERRO")
        );

        assertEquals("El archivo no puede estar vacío.", ex.getMessage());
        verify(laboratorioIAClient, never()).analizar(file, "PERRO");
    }

    @Test
    void analizarLaboratorio_rechazaExtensionNoSoportada() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.exe", "application/octet-stream", "x".getBytes());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarLaboratorio(file, "GATO")
        );

        assertEquals("Formato no soportado: \".exe\". Use PDF, JPG, PNG, BMP o TIFF.", ex.getMessage());
        verify(laboratorioIAClient, never()).analizar(file, "GATO");
    }

    @Test
    void analizarLaboratorio_retornaRespuestaDelClienteCuandoArchivoEsValido() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "pdf".getBytes());
        LaboratorioIAResponse expected = new LaboratorioIAResponse();
        expected.setEspecie("PERRO");
        when(laboratorioIAClient.analizar(file, "PERRO")).thenReturn(expected);

        LaboratorioIAResponse response = service.analizarLaboratorio(file, "PERRO");

        assertSame(expected, response);
    }

    @Test
    void analizarLaboratorio_envuelveErrorDelCliente() throws Exception {
        LaboratorioIAServiceImpl service = new LaboratorioIAServiceImpl(laboratorioIAClient);
        MultipartFile file = new MockMultipartFile("archivo", "hemograma.pdf", "application/pdf", "pdf".getBytes());
        when(laboratorioIAClient.analizar(file, "PERRO")).thenThrow(new IOException("timeout"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.analizarLaboratorio(file, "PERRO")
        );

        assertEquals("Error al conectar con el servicio de análisis de laboratorio: timeout", ex.getMessage());
    }

    @Test
    void analizarRadiografia_rechazaArchivoVacio() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient);
        MultipartFile file = new MockMultipartFile("file", "rx.dcm", "application/dicom", new byte[0]);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarRadiografia(file)
        );

        assertEquals("El archivo de radiografía no puede estar vacío.", ex.getMessage());
        verify(radiografiaIAClient, never()).predict(file);
    }

    @Test
    @DisplayName("[BB-014] Analizar radiografia con formato invalido es rechazado")
    void analizarRadiografia_rechazaFormatoNoSoportado() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient);
        MultipartFile file = new MockMultipartFile("file", "rx.pdf", "application/pdf", "pdf".getBytes());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.analizarRadiografia(file)
        );

        assertEquals("Formato no soportado. Use DICOM (.dcm), PNG o JPG.", ex.getMessage());
        verify(radiografiaIAClient, never()).predict(file);
    }

    @Test
    void analizarRadiografia_retornaRespuestaDelClienteCuandoArchivoEsValido() throws Exception {
        RadiografiaIAServiceImpl service = new RadiografiaIAServiceImpl(radiografiaIAClient);
        MultipartFile file = new MockMultipartFile("file", "rx.dcm", "application/dicom", "dicom".getBytes());
        RadiografiaPrediccionResponse expected = new RadiografiaPrediccionResponse();
        expected.setModel("rx-model");
        when(radiografiaIAClient.predict(file)).thenReturn(expected);

        RadiografiaPrediccionResponse response = service.analizarRadiografia(file);

        assertSame(expected, response);
    }
}
