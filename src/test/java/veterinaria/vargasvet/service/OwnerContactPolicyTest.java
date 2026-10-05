package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.service.impl.UsuarioContactoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Los avisos con datos de la mascota o la cita solo van a un correo que la persona ya confirmó. */
class OwnerContactPolicyTest {

    private final UsuarioContactoService contactoService = mock(UsuarioContactoService.class);
    private final OwnerContactPolicy policy = new OwnerContactPolicy(contactoService);

    private Usuario usuario(String email, boolean verificado) {
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setEmail(email);
        usuario.setEmailVerified(verificado);
        return usuario;
    }

    @Test
    void soloUnCorreoVerificadoRecibeAvisos() {
        assertThat(policy.puedeRecibirCorreo(usuario("ana@example.test", true))).isTrue();
        assertThat(policy.puedeRecibirCorreo(usuario("ana@example.test", false))).isFalse();
    }

    @Test
    void sinCorreoOSinPersonaNoHayAvisoPorCorreo() {
        assertThat(policy.puedeRecibirCorreo(usuario("  ", true))).isFalse();
        assertThat(policy.puedeRecibirCorreo(usuario(null, true))).isFalse();
        assertThat(policy.puedeRecibirCorreo(null)).isFalse();
    }

    @Test
    void elTelefonoSeBuscaEnLaEmpresaDeEsaRelacionDeCliente() {
        Company clinica = new Company();
        clinica.setId(7);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario("ana@example.test", false));
        apoderado.setCompany(clinica);
        when(contactoService.telefono(20, 7)).thenReturn("987654321");

        assertThat(policy.telefono(apoderado)).isEqualTo("987654321");
    }

    @Test
    void sinPersonaNoSeConsultaNingunTelefono() {
        assertThat(policy.telefono(null)).isNull();
        assertThat(policy.telefono(new Apoderado())).isNull();

        verifyNoInteractions(contactoService);
    }
}
