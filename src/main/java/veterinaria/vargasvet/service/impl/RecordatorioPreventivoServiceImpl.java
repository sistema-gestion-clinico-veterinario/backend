package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.RecordatorioPreventivoService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario;
import veterinaria.vargasvet.service.recordatorios.ComponedorDelCorreoPreventivo;
import veterinaria.vargasvet.service.recordatorios.PlanificadorDeRecordatoriosPreventivos;
import veterinaria.vargasvet.service.recordatorios.RegistroDeRecordatoriosEnviados;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordatorioPreventivoServiceImpl implements RecordatorioPreventivoService {

    private static final String PLANTILLA = "email/recordatorio-preventivo-template";

    private final PlanificadorDeRecordatoriosPreventivos planificador;
    private final ComponedorDelCorreoPreventivo componedor;
    private final RegistroDeRecordatoriosEnviados registro;
    private final EmailService emailService;

    private enum Resultado { ENVIADO, NO_ENVIADO, ENVIADO_SIN_REGISTRO }

    @Override
    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}", zone = "${app.reminders.zone:America/Lima}")
    public void procesarRecordatorios() {
        LocalDate hoy = AppClock.today();
        List<AvisosParaPropietario> pendientes = planificador.planificar(hoy);

        int enviados = 0;
        int noEnviados = 0;
        int sinRegistro = 0;
        for (AvisosParaPropietario propietario : pendientes) {
            switch (procesar(propietario, hoy)) {
                case ENVIADO -> enviados++;
                case NO_ENVIADO -> noEnviados++;
                case ENVIADO_SIN_REGISTRO -> {
                    enviados++;
                    sinRegistro++;
                }
            }
        }
        if (!pendientes.isEmpty()) {
            log.info("Recordatorios preventivos: {} propietarios, {} enviados, {} no enviados (se reintentan en la próxima "
                    + "ejecución), {} enviados sin registrar.", pendientes.size(), enviados, noEnviados, sinRegistro);
        }
    }

    private Resultado procesar(AvisosParaPropietario propietario, LocalDate hoy) {
        try {
            if (!enviar(componedor.componer(propietario, hoy))) {
                log.warn("No se pudo enviar el recordatorio preventivo al propietario {}; se reintentará en la próxima "
                        + "ejecución programada.", propietario.apoderadoId());
                return Resultado.NO_ENVIADO;
            }
        } catch (RuntimeException e) {
            log.error("No se pudo preparar el recordatorio preventivo del propietario {}.", propietario.apoderadoId(), e);
            return Resultado.NO_ENVIADO;
        }
        try {
            registro.registrar(propietario);
            return Resultado.ENVIADO;
        } catch (RuntimeException e) {
            log.error("El recordatorio preventivo del propietario {} se envió pero no se pudo registrar; podría repetirse "
                    + "en la próxima ejecución.", propietario.apoderadoId(), e);
            return Resultado.ENVIADO_SIN_REGISTRO;
        }
    }

    private boolean enviar(Mail correo) {
        try {
            return emailService.sendEmail(correo, PLANTILLA).join();
        } catch (RuntimeException e) {
            log.error("Error inesperado al enviar un recordatorio preventivo.", e);
            return false;
        }
    }
}
