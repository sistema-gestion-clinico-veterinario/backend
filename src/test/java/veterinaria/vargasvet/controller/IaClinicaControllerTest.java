package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import veterinaria.vargasvet.dto.response.AutorizacionIaResponse;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.exception.AutorizacionIaRequeridaException;
import veterinaria.vargasvet.integration.DiagnosticoIAClient;
import veterinaria.vargasvet.integration.DiagnosticoIAClient.ArchivoParte;
import veterinaria.vargasvet.service.AutorizacionIaService;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IaClinicaControllerTest {

    private final AutorizacionIaService autorizaciones = mock(AutorizacionIaService.class);
    private final DiagnosticoIAClient cliente = mock(DiagnosticoIAClient.class);
    private final IaClinicaController controller = new IaClinicaController(autorizaciones, cliente);
    private final AutorizacionIaResponse vigente =
            new AutorizacionIaResponse(true, 5L, "Ana Pérez", LocalDateTime.of(2026, 10, 6, 10, 0), CanalConsentimiento.PORTAL);

    @Test
    void sinAutorizacionNoSeLeEnviaNadaAlServicioDeIa() {
        when(autorizaciones.exigir(7L)).thenThrow(new AutorizacionIaRequeridaException(5L));

        assertThatThrownBy(() -> controller.diagnostico(7L, Map.of("motivo_consulta", "tos"), null, null))
                .isInstanceOf(AutorizacionIaRequeridaException.class);

        verifyNoInteractions(cliente);
        verify(autorizaciones, never()).registrarUso(any(), any(), any());
    }

    @Test
    void conAutorizacionReenviaSoloLosCamposPermitidosYLosArchivosYDejaAuditoria() throws Exception {
        when(autorizaciones.exigir(7L)).thenReturn(vigente);
        MockMultipartFile hemograma = new MockMultipartFile("archivo_hemograma", "hemo.pdf", "application/pdf", new byte[]{1, 2});
        MockMultipartFile vacio = new MockMultipartFile("archivo_radiografia", "vacio.png", "image/png", new byte[0]);
        MockMultipartFile radio = new MockMultipartFile("archivo_radiografia", "rx.png", "image/png", new byte[]{3});
        Map<String, String> campos = Map.of("motivo_consulta", "tos", "especie", "Perro", "mascotaId", "7",
                "campo_inventado", "x", "nombre_paciente", "Luna");

        var respuesta = controller.diagnostico(7L, campos, List.of(hemograma), List.of(vacio, radio));
        respuesta.getBody().writeTo(new ByteArrayOutputStream());

        org.mockito.ArgumentCaptor<Map<String, String>> enviados = org.mockito.ArgumentCaptor.forClass(Map.class);
        org.mockito.ArgumentCaptor<List<ArchivoParte>> archivos = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(cliente).transmitir(enviados.capture(), archivos.capture(), any());
        assertThat(enviados.getValue()).containsOnlyKeys("motivo_consulta", "especie", "nombre_paciente");
        assertThat(archivos.getValue()).extracting(ArchivoParte::campo, ArchivoParte::nombre)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("archivo_hemograma", "hemo.pdf"),
                        org.assertj.core.groups.Tuple.tuple("archivo_radiografia", "rx.png"));
        verify(autorizaciones).registrarUso(7L, "diagnóstico", vigente);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
        assertThat(respuesta.getHeaders().getFirst("Cache-Control")).isEqualTo("no-cache");
    }

    @Test
    void siElServicioDeIaFallaLaPantallaRecibeUnEventoDeErrorSinDetalles() throws Exception {
        when(autorizaciones.exigir(7L)).thenReturn(vigente);
        doThrow(new IllegalStateException("conexión rechazada a https://interno")).when(cliente)
                .transmitir(anyMap(), anyList(), any());

        var respuesta = controller.diagnostico(7L, Map.of("motivo_consulta", "tos"), null, null);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        respuesta.getBody().writeTo(salida);

        String texto = salida.toString(StandardCharsets.UTF_8);
        assertThat(texto).contains("\"type\":\"error\"").doesNotContain("interno").doesNotContain("rechazada");
    }

    @Test
    void laConsultaDeEstadoDevuelveLaAutorizacionDeLaMascota() {
        when(autorizaciones.estado(7L)).thenReturn(vigente);

        var respuesta = controller.autorizacion(7L);

        assertThat(respuesta.getBody().getData()).isSameAs(vigente);
    }
}
