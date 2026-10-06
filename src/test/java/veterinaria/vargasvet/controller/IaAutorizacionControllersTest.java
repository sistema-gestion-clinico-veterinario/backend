package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.dto.response.AutorizacionIaResponse;
import veterinaria.vargasvet.dto.response.LaboratorioIAResponse;
import veterinaria.vargasvet.dto.response.RadiografiaPrediccionResponse;
import veterinaria.vargasvet.exception.AutorizacionIaRequeridaException;
import veterinaria.vargasvet.service.AutorizacionIaService;
import veterinaria.vargasvet.service.LaboratorioIAService;
import veterinaria.vargasvet.service.RadiografiaIAService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IaAutorizacionControllersTest {

    private final AutorizacionIaService autorizaciones = mock(AutorizacionIaService.class);
    private final LaboratorioIAService laboratorio = mock(LaboratorioIAService.class);
    private final RadiografiaIAService radiografia = mock(RadiografiaIAService.class);
    private final LaboratorioIAController laboratorioController = new LaboratorioIAController(laboratorio, autorizaciones);
    private final RadiografiaIAController radiografiaController = new RadiografiaIAController(radiografia, autorizaciones);
    private final MockMultipartFile archivo = new MockMultipartFile("archivo", "hemo.pdf", "application/pdf", new byte[]{1});
    private final AutorizacionIaResponse vigente =
            new AutorizacionIaResponse(true, 5L, "Ana Pérez", LocalDateTime.of(2026, 10, 6, 10, 0), CanalConsentimiento.PRESENCIAL);

    @Test
    void elLaboratorioNoSeAnalizaSinAutorizacionDelTitular() {
        when(autorizaciones.exigir(7L)).thenThrow(new AutorizacionIaRequeridaException(5L));

        assertThatThrownBy(() -> laboratorioController.analizarLaboratorio(archivo, 7L, "Perro"))
                .isInstanceOf(AutorizacionIaRequeridaException.class);

        verifyNoInteractions(laboratorio);
        verify(autorizaciones, never()).registrarUso(any(), any(), any());
    }

    @Test
    void elLaboratorioConAutorizacionSeAnalizaYQuedaEnLaAuditoria() {
        when(autorizaciones.exigir(7L)).thenReturn(vigente);
        LaboratorioIAResponse resultado = new LaboratorioIAResponse();
        when(laboratorio.analizarLaboratorio(archivo, "Gato")).thenReturn(resultado);

        var respuesta = laboratorioController.analizarLaboratorio(archivo, 7L, "Gato");

        assertThat(respuesta.getBody().getData()).isSameAs(resultado);
        verify(autorizaciones).registrarUso(7L, "laboratorio", vigente);
    }

    @Test
    void laRadiografiaNoSeAnalizaSinAutorizacionDelTitular() {
        when(autorizaciones.exigir(7L)).thenThrow(new AutorizacionIaRequeridaException(5L));

        assertThatThrownBy(() -> radiografiaController.analizarRadiografia(archivo, 7L))
                .isInstanceOf(AutorizacionIaRequeridaException.class);

        verifyNoInteractions(radiografia);
    }

    @Test
    void laRadiografiaConAutorizacionSeAnalizaYQuedaEnLaAuditoria() {
        when(autorizaciones.exigir(7L)).thenReturn(vigente);
        RadiografiaPrediccionResponse resultado = new RadiografiaPrediccionResponse();
        when(radiografia.analizarRadiografia(archivo)).thenReturn(resultado);

        var respuesta = radiografiaController.analizarRadiografia(archivo, 7L);

        assertThat(respuesta.getBody().getData()).isSameAs(resultado);
        verify(autorizaciones).registrarUso(7L, "radiografía", vigente);
    }
}
