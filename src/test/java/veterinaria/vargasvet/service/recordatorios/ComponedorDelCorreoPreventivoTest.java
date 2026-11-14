package veterinaria.vargasvet.service.recordatorios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Aviso;
import veterinaria.vargasvet.service.recordatorios.AvisosParaPropietario.Clinica;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponedorDelCorreoPreventivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Mock EmailService emailService;
    private ComponedorDelCorreoPreventivo componedor;
    private final Mail correo = new Mail();

    @BeforeEach
    void preparar() {
        componedor = new ComponedorDelCorreoPreventivo(emailService);
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(correo);
    }

    @Test
    void elCorreoVaAlPropietarioConElNombreDeLaClinicaEnElAsunto() {
        Mail resultado = componedor.componer(propietario(clinica(), aviso(HOY.plusDays(3), TipoAvisoRecordatorio.PROXIMO)), HOY);

        assertThat(resultado).isSameAs(correo);
        verify(emailService).createMail(eq("ana@example.test"), eq("Controles preventivos de sus mascotas - Patitas Felices"),
                anyMap());
    }

    @Test
    void elModeloLlevaLosDatosDelPropietarioYDeLaClinica() {
        Map<String, Object> modelo = modeloDe(propietario(clinica(), aviso(HOY.plusDays(3), TipoAvisoRecordatorio.PROXIMO)));

        assertThat(modelo).containsEntry("nombre", "Ana Perez").containsEntry("totalControles", 1)
                .containsEntry("fechaProceso", HOY).containsEntry("companyName", "Patitas Felices")
                .containsEntry("companyLogo", "https://logo.test/patitas.png").containsEntry("companyEmail", "hola@patitas.test")
                .containsEntry("companyPhone", "999111222").containsEntry("companyAddress", "Av. Lima 123");
    }

    @Test
    void sinClinicaUsaUnNombreGenericoYNoAgregaDatosDeContacto() {
        Map<String, Object> modelo = modeloDe(propietario(null, aviso(HOY.plusDays(3), TipoAvisoRecordatorio.PROXIMO)));

        assertThat(modelo).containsEntry("companyName", "su veterinaria")
                .doesNotContainKeys("companyLogo", "companyEmail", "companyPhone", "companyAddress");
        verify(emailService).createMail(anyString(), eq("Controles preventivos de sus mascotas - su veterinaria"), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cadaControlDiceCuantosDiasFaltanOHanPasado() {
        Map<String, Object> modelo = modeloDe(propietario(clinica(),
                aviso(HOY.minusDays(2), TipoAvisoRecordatorio.ATRASADO),
                aviso(HOY, TipoAvisoRecordatorio.PENDIENTE),
                aviso(HOY.plusDays(3), TipoAvisoRecordatorio.PROXIMO)));

        List<Map<String, Object>> controles = (List<Map<String, Object>>) modelo.get("controles");
        assertThat(controles).extracting(control -> control.get("resumenDias"))
                .containsExactly("Hace 2 día(s)", "Es hoy", "En 3 día(s)");
        assertThat(controles).extracting(control -> control.get("estado")).containsExactly("ATRASADO", "PENDIENTE", "PROXIMO");
        assertThat(controles).extracting(control -> control.get("esAtrasado")).containsExactly(true, false, false);
        assertThat(controles).extracting(control -> control.get("esPendiente")).containsExactly(false, true, false);
        assertThat(controles).extracting(control -> control.get("esProximo")).containsExactly(false, false, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cadaControlLlevaMascotaNombreTipoYFecha() {
        Map<String, Object> modelo = modeloDe(propietario(clinica(), aviso(HOY.plusDays(3), TipoAvisoRecordatorio.PROXIMO),
                new Aviso(2L, "Luna", "Desparasitación interna", TipoControlPreventivo.DESPARASITACION, HOY.plusDays(5),
                        TipoAvisoRecordatorio.PROXIMO)));

        List<Map<String, Object>> controles = (List<Map<String, Object>>) modelo.get("controles");
        assertThat(controles.get(0)).containsEntry("mascota", "Rocky").containsEntry("control", "Antirrabica")
                .containsEntry("tipo", "VACUNACION").containsEntry("tipoDisplay", "Vacunación")
                .containsEntry("fecha", HOY.plusDays(3));
        assertThat(controles.get(1)).containsEntry("tipo", "DESPARASITACION").containsEntry("tipoDisplay", "Desparasitación");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> modeloDe(AvisosParaPropietario propietario) {
        componedor.componer(propietario, HOY);
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(anyString(), anyString(), modelo.capture());
        return modelo.getValue();
    }

    private AvisosParaPropietario propietario(Clinica clinica, Aviso... avisos) {
        return new AvisosParaPropietario(5L, "ana@example.test", "Ana Perez", clinica, List.of(avisos));
    }

    private Clinica clinica() {
        return new Clinica("Patitas Felices", "https://logo.test/patitas.png", "hola@patitas.test", "999111222", "Av. Lima 123");
    }

    private Aviso aviso(LocalDate fecha, TipoAvisoRecordatorio tipoAviso) {
        return new Aviso(1L, "Rocky", "Antirrabica", TipoControlPreventivo.VACUNACION, fecha, tipoAviso);
    }
}
