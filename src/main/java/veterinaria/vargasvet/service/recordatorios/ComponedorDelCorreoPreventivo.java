package veterinaria.vargasvet.service.recordatorios;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Aviso;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ComponedorDelCorreoPreventivo {

    private static final String CLINICA_SIN_NOMBRE = "su veterinaria";

    private final EmailService emailService;

    public Mail componer(AvisosParaPropietario propietario, LocalDate hoy) {
        String nombreClinica = propietario.clinica() == null ? CLINICA_SIN_NOMBRE : propietario.clinica().nombre();
        List<Map<String, Object>> controles = propietario.avisos().stream().map(aviso -> controlDelCorreo(aviso, hoy)).toList();

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("nombre", propietario.nombre());
        modelo.put("controles", controles);
        modelo.put("totalControles", controles.size());
        modelo.put("fechaProceso", hoy);
        modelo.put("companyName", nombreClinica);
        if (propietario.clinica() != null) {
            modelo.put("companyLogo", propietario.clinica().logoUrl());
            modelo.put("companyEmail", propietario.clinica().correo());
            modelo.put("companyPhone", propietario.clinica().telefono());
            modelo.put("companyAddress", propietario.clinica().direccion());
        }
        return emailService.createMail(propietario.correo(),
                "Controles preventivos de sus mascotas - " + nombreClinica, modelo);
    }

    private Map<String, Object> controlDelCorreo(Aviso aviso, LocalDate hoy) {
        Map<String, Object> item = new HashMap<>();
        item.put("mascota", aviso.mascota());
        item.put("control", aviso.control());
        item.put("tipo", aviso.tipo().name());
        item.put("tipoDisplay", aviso.tipo() == TipoControlPreventivo.VACUNACION ? "Vacunación" : "Desparasitación");
        item.put("fecha", aviso.fechaRecomendada());
        item.put("estado", aviso.tipoAviso().name());
        item.put("resumenDias", resumenDeDias(ChronoUnit.DAYS.between(hoy, aviso.fechaRecomendada())));
        item.put("esAtrasado", aviso.tipoAviso() == TipoAvisoRecordatorio.ATRASADO);
        item.put("esPendiente", aviso.tipoAviso() == TipoAvisoRecordatorio.PENDIENTE);
        item.put("esProximo", aviso.tipoAviso() == TipoAvisoRecordatorio.PROXIMO);
        return item;
    }

    private String resumenDeDias(long dias) {
        if (dias < 0) return "Hace " + Math.abs(dias) + " día(s)";
        if (dias == 0) return "Es hoy";
        return "En " + dias + " día(s)";
    }
}
