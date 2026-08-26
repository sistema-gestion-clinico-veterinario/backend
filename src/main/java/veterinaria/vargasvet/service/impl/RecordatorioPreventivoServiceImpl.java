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
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
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
            if (tipoAviso == null || recordatorioRepository
                    .existsByControlPreventivoIdAndTipoAvisoAndFechaProgramada(
                            control.getId(), tipoAviso, control.getFechaRecomendada())) {
                continue;
            }
            Long apoderadoId = control.getMascota().getApoderado().getId();
            porApoderado.computeIfAbsent(apoderadoId, ignored -> new ArrayList<>())
                    .add(new AvisoPendiente(control, tipoAviso));
        }

        porApoderado.forEach((apoderadoId, avisos) -> {
            enviarConsolidado(avisos, hoy);
        });
    }

    private void enviarConsolidado(List<AvisoPendiente> avisos, LocalDate hoy) {
        if (avisos.isEmpty()) return;
        avisos.sort(Comparator.comparing((AvisoPendiente a) -> prioridad(a.tipoAviso()))
                .thenComparing(a -> a.control().getFechaRecomendada()));
        var usuario = avisos.get(0).control().getMascota().getApoderado().getUser();
        var company = usuario.getCompany();

        List<Map<String, Object>> controles = avisos.stream().map(aviso -> {
            ControlPreventivo c = aviso.control();
            LocalDate fecha = c.getFechaRecomendada();
            long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, fecha);
            String resumenDias;
            if (dias < 0) resumenDias = "Hace " + Math.abs(dias) + " día(s)";
            else if (dias == 0) resumenDias = "Es hoy";
            else resumenDias = "En " + dias + " día(s)";

            Map<String, Object> item = new HashMap<>();
            item.put("mascota", c.getMascota().getNombreCompleto());
            item.put("control", c.getNombreControl());
            item.put("tipo", c.getTipo().name());
            item.put("tipoDisplay", c.getTipo() == TipoControlPreventivo.VACUNACION
                    ? "Vacunación" : "Desparasitación");
            item.put("fecha", fecha);
            item.put("estado", aviso.tipoAviso().name());
            item.put("resumenDias", resumenDias);
            item.put("esAtrasado", aviso.tipoAviso() == TipoAvisoRecordatorio.ATRASADO);
            item.put("esPendiente", aviso.tipoAviso() == TipoAvisoRecordatorio.PENDIENTE);
            item.put("esProximo", aviso.tipoAviso() == TipoAvisoRecordatorio.PROXIMO);
            return item;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("nombre", (usuario.getNombre() + " " + usuario.getApellido()).trim());
        model.put("controles", controles);
        model.put("totalControles", controles.size());
        model.put("fechaProceso", hoy);
        String companyName = company == null ? "su veterinaria" : company.getName();
        model.put("companyName", companyName);
        if (company != null) {
            model.put("companyLogo", company.getLogoUrl());
            model.put("companyEmail", company.getEmail());
            model.put("companyPhone", company.getPhone());
            model.put("companyAddress", company.getAddress());
        }
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
        if (hoy.isAfter(fecha)) return TipoAvisoRecordatorio.ATRASADO;
        if (hoy.isEqual(fecha)) return TipoAvisoRecordatorio.PENDIENTE;
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
