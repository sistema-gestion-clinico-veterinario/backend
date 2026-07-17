package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.RecordatorioPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.RecordatorioPreventivoRepository;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.RecordatorioPreventivoService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordatorioPreventivoServiceImpl implements RecordatorioPreventivoService {
    private final ControlPreventivoRepository controlRepository;
    private final RecordatorioPreventivoRepository recordatorioRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}", zone = "${app.reminders.zone:America/Lima}")
    public void procesarRecordatorios() {
        LocalDate hoy = AppClock.today();
        List<ControlPreventivo> candidatos = controlRepository.findReminderCandidates(
                hoy.plusDays(7), EnumSet.of(EstadoControlPreventivo.PROGRAMADO,
                        EstadoControlPreventivo.PROXIMO, EstadoControlPreventivo.PENDIENTE,
                        EstadoControlPreventivo.ATRASADO));

        Map<Long, List<AvisoPendiente>> porApoderado = new LinkedHashMap<>();
        for (ControlPreventivo control : candidatos) {
            TipoAvisoRecordatorio tipoAviso = determinarAviso(control, hoy);
            actualizarEstado(control, hoy);
            if (tipoAviso == null || recordatorioRepository.existsByControlPreventivoIdAndTipoAviso(control.getId(), tipoAviso)) {
                continue;
            }
            Long apoderadoId = control.getMascota().getApoderado().getId();
            porApoderado.computeIfAbsent(apoderadoId, ignored -> new ArrayList<>())
                    .add(new AvisoPendiente(control, tipoAviso));
        }

        LocalDateTime limiteFrecuencia = AppClock.now().minusDays(7);
        porApoderado.forEach((apoderadoId, avisos) -> {
            if (recordatorioRepository.countByApoderadoIdAndFechaEnvioAfter(apoderadoId, limiteFrecuencia) > 0) {
                return;
            }
            enviarConsolidado(avisos, hoy);
        });
    }

    private void enviarConsolidado(List<AvisoPendiente> avisos, LocalDate hoy) {
        if (avisos.isEmpty()) return;
        avisos.sort(Comparator.comparing((AvisoPendiente a) -> prioridad(a.tipoAviso()))
                .thenComparing(a -> a.control().getFechaRecomendada()));
        var usuario = avisos.get(0).control().getMascota().getApoderado().getUser();
        List<Map<String, Object>> controles = avisos.stream().map(aviso -> {
            Map<String, Object> item = new HashMap<>();
            item.put("mascota", aviso.control().getMascota().getNombreCompleto());
            item.put("control", aviso.control().getNombreControl());
            item.put("tipo", aviso.control().getTipo().name());
            item.put("fecha", aviso.control().getFechaRecomendada());
            item.put("estado", aviso.tipoAviso().name());
            return item;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("nombre", (usuario.getNombre() + " " + usuario.getApellido()).trim());
        model.put("controles", controles);
        model.put("fechaProceso", hoy);
        String companyName = usuario.getCompany() == null ? "su veterinaria" : usuario.getCompany().getName();
        model.put("companyName", companyName);
        Mail mail = emailService.createMail(usuario.getEmail(), "Controles preventivos de sus mascotas - " + companyName, model);
        emailService.sendEmail(mail, "email/recordatorio-preventivo-template");

        LocalDateTime enviadoAt = AppClock.now();
        for (AvisoPendiente aviso : avisos) {
            RecordatorioPreventivo registro = new RecordatorioPreventivo();
            registro.setApoderado(aviso.control().getMascota().getApoderado());
            registro.setControlPreventivo(aviso.control());
            registro.setTipoAviso(aviso.tipoAviso());
            registro.setFechaProgramada(aviso.control().getFechaRecomendada());
            registro.setFechaEnvio(enviadoAt);
            registro.setEstado(EstadoRecordatorio.ENVIADO);
            registro.setCreatedBy("SYSTEM");
            registro.setUpdatedBy("SYSTEM");
            recordatorioRepository.save(registro);
        }
        log.info("Recordatorio preventivo consolidado enviado a {} con {} controles", usuario.getEmail(), avisos.size());
    }

    private TipoAvisoRecordatorio determinarAviso(ControlPreventivo control, LocalDate hoy) {
        LocalDate fecha = control.getFechaRecomendada();
        if (!hoy.isBefore(fecha.plusDays(7))) return TipoAvisoRecordatorio.ATRASADO;
        if (!hoy.isBefore(fecha)) return TipoAvisoRecordatorio.PENDIENTE;
        if (!hoy.isBefore(fecha.minusDays(7))) return TipoAvisoRecordatorio.PROXIMO;
        return null;
    }

    private void actualizarEstado(ControlPreventivo control, LocalDate hoy) {
        EstadoControlPreventivo nuevo = control.getFechaRecomendada().isBefore(hoy)
                ? EstadoControlPreventivo.ATRASADO
                : control.getFechaRecomendada().isEqual(hoy) ? EstadoControlPreventivo.PENDIENTE
                : !control.getFechaRecomendada().isAfter(hoy.plusDays(7))
                ? EstadoControlPreventivo.PROXIMO : EstadoControlPreventivo.PROGRAMADO;
        if (control.getEstado() != nuevo) {
            control.setEstado(nuevo);
            control.setEstadoModificadoPor("SYSTEM");
            control.setFechaModificacionEstado(AppClock.now());
            control.setUpdatedBy("SYSTEM");
        }
    }

    private int prioridad(TipoAvisoRecordatorio tipo) {
        return switch (tipo) {
            case ATRASADO -> 0;
            case PENDIENTE -> 1;
            case PROXIMO -> 2;
        };
    }

    private record AvisoPendiente(ControlPreventivo control, TipoAvisoRecordatorio tipoAviso) {}
}
