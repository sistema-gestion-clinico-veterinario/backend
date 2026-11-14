package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario;
import veterinaria.vargasvet.service.recordatorios.ComponedorDelCorreoPreventivo;
import veterinaria.vargasvet.service.recordatorios.PlanificadorDeRecordatoriosPreventivos;
import veterinaria.vargasvet.service.recordatorios.RegistroDeRecordatoriosEnviados;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioPreventivoProcesoTest {

    private static final String PLANTILLA = "email/recordatorio-preventivo-template";

    @Mock PlanificadorDeRecordatoriosPreventivos planificador;
    @Mock ComponedorDelCorreoPreventivo componedor;
    @Mock RegistroDeRecordatoriosEnviados registro;
    @Mock EmailService emailService;

    private RecordatorioPreventivoServiceImpl service;
    private AvisosParaPropietario ana;
    private AvisosParaPropietario beto;
    private AvisosParaPropietario carla;

    @BeforeEach
    void preparar() {
        service = new RecordatorioPreventivoServiceImpl(planificador, componedor, registro, emailService);
        ana = propietario(1L);
        beto = propietario(2L);
        carla = propietario(3L);
        lenient().when(planificador.planificar(any(LocalDate.class))).thenReturn(List.of(ana, beto, carla));
        lenient().when(componedor.componer(any(), any())).thenAnswer(i -> new Mail());
        lenient().when(emailService.sendEmail(any(Mail.class), eq(PLANTILLA)))
                .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    void registraALosAvisosDeCadaPropietarioDespuesDeEnviarleElCorreo() {
        service.procesarRecordatorios();

        InOrder orden = inOrder(emailService, registro);
        for (AvisosParaPropietario propietario : List.of(ana, beto, carla)) {
            orden.verify(emailService).sendEmail(any(Mail.class), eq(PLANTILLA));
            orden.verify(registro).registrar(propietario);
        }
    }

    @Test
    void siNoHayNadaQueAvisarNoEnviaNiRegistra() {
        when(planificador.planificar(any(LocalDate.class))).thenReturn(List.of());

        service.procesarRecordatorios();

        verifyNoInteractions(componedor, emailService, registro);
    }

    @Test
    void siElCorreoFallaNoRegistraAlPropietarioYSigueConLosDemas() {
        when(emailService.sendEmail(any(Mail.class), eq(PLANTILLA)))
                .thenReturn(CompletableFuture.completedFuture(true))
                .thenReturn(CompletableFuture.completedFuture(false))
                .thenReturn(CompletableFuture.completedFuture(true));

        service.procesarRecordatorios();

        verify(registro).registrar(ana);
        verify(registro, never()).registrar(beto);
        verify(registro).registrar(carla);
    }

    @Test
    void siElEnvioLanzaUnaExcepcionNoRegistraAlPropietarioYSigueConLosDemas() {
        when(emailService.sendEmail(any(Mail.class), eq(PLANTILLA)))
                .thenReturn(CompletableFuture.completedFuture(true))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("servidor de correo caído")))
                .thenReturn(CompletableFuture.completedFuture(true));

        service.procesarRecordatorios();

        verify(registro).registrar(ana);
        verify(registro, never()).registrar(beto);
        verify(registro).registrar(carla);
    }

    @Test
    void siFallaElRegistroDeUnPropietarioLosDemasSeEnviaYSeRegistranIgual() {
        lenient().doThrow(new IllegalStateException("fallo al guardar")).when(registro).registrar(beto);

        service.procesarRecordatorios();

        verify(emailService, times(3)).sendEmail(any(Mail.class), eq(PLANTILLA));
        verify(registro).registrar(ana);
        verify(registro).registrar(beto);
        verify(registro).registrar(carla);
    }

    @Test
    void siFallaLaComposicionDelCorreoDeUnPropietarioNoLeEnviaNadaYSigueConLosDemas() {
        when(componedor.componer(eq(beto), any())).thenThrow(new IllegalStateException("datos incompletos"));

        service.procesarRecordatorios();

        verify(emailService, times(2)).sendEmail(any(Mail.class), eq(PLANTILLA));
        verify(registro, never()).registrar(beto);
        verify(registro).registrar(ana);
        verify(registro).registrar(carla);
    }

    private AvisosParaPropietario propietario(Long apoderadoId) {
        return new AvisosParaPropietario(apoderadoId, "propietario" + apoderadoId + "@example.test", "Propietario " + apoderadoId,
                null, List.of());
    }
}
