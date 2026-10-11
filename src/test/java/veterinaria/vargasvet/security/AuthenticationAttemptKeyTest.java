package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationAttemptKeyTest {

    @Test
    void laMismaIdentidadEnDosEmpresasProduceCredencialesDeSeguridadDistintas() {
        assertThat(AuthenticationAttemptKey.forCompany(7, " Persona@Example.com "))
                .isEqualTo("company:7:persona@example.com")
                .isNotEqualTo(AuthenticationAttemptKey.forCompany(8, "persona@example.com"));
    }

    @Test
    void laCuentaDePlataformaNoComparteContadorConUnaClinica() {
        assertThat(AuthenticationAttemptKey.forPlatform("ADMIN"))
                .isEqualTo("platform:admin")
                .isNotEqualTo(AuthenticationAttemptKey.forCompany(7, "admin"));
    }

    @Test
    void usuarioYCorreoConvergenEnLaMismaCuentaInterna() {
        assertThat(AuthenticationAttemptKey.forCompanyAccount(7, 10))
                .isEqualTo("company:7:account:10");
    }

    @Test
    void exigeEmpresaParaUnaCredencialDeClinica() {
        assertThatThrownBy(() -> AuthenticationAttemptKey.forCompany(null, "ana"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
