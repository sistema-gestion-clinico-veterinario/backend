package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetLinkNotifierTest {

    private EmailService emailService;
    private PetLinkNotifier notifier;
    private Apoderado principal;
    private Apoderado persona;
    private Mascota luna;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenReturn(new Mail());
        notifier = new PetLinkNotifier(emailService, new OwnerContactPolicy(mock(UsuarioContactoService.class)));
        Company clinica = new Company();
        clinica.setId(7);
        clinica.setName("Clínica Patitas");
        principal = apoderado(1L, "Maria", "maria@example.test", true, clinica);
        persona = apoderado(2L, "Carlos", "carlos@example.test", true, clinica);
        luna = new Mascota();
        luna.setId(10L);
        luna.setNombreCompleto("Luna");
        luna.setApoderado(principal);
    }

    private Apoderado apoderado(Long id, String nombre, String correo, boolean verificado, Company empresa) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido("Test");
        usuario.setEmail(correo);
        usuario.setEmailVerified(verificado);
        Apoderado apoderado = new Apoderado();
        apoderado.setId(id);
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        apoderado.setEstado(true);
        return apoderado;
    }

    @Test
    void alPropietarioConCorreoVerificadoSeLeAvisaQuienSeVinculoYConQuePermisos() {
        notifier.avisarAlPrincipal(luna, persona, "vinculó", "recibir información: sí");

        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(emailService).createMail(eq("maria@example.test"), eq("Cambio en las personas vinculadas a Luna"), modelo.capture());
        assertThat(modelo.getValue()).containsEntry("persona", "Carlos Test").containsEntry("accion", "vinculó")
                .containsEntry("mascota", "Luna").containsEntry("detalle", "recibir información: sí");
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/vinculo-mascota-template"));
    }

    @Test
    void noSeAvisaSiElPropietarioNoTieneCorreoVerificadoEstaDeBajaOLaPersonaEsElMismo() {
        principal.getUser().setEmailVerified(false);
        notifier.avisarAlPrincipal(luna, persona, "vinculó", "x");

        principal.getUser().setEmailVerified(true);
        principal.setEstado(false);
        notifier.avisarAlPrincipal(luna, persona, "vinculó", "x");

        principal.setEstado(true);
        notifier.avisarAlPrincipal(luna, principal, "vinculó", "x");

        verify(emailService, never()).sendEmailWithRetry(any(), anyString());
    }

    @Test
    void siElCorreoFallaElCambioNoSeVeAfectado() {
        when(emailService.createMail(anyString(), anyString(), anyMap())).thenThrow(new IllegalStateException("sin correo"));

        notifier.avisarAlPrincipal(luna, persona, "revocó", "x");

        verify(emailService, never()).sendEmailWithRetry(any(), anyString());
    }
}
