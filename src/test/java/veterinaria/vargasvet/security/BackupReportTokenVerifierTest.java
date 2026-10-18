package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackupReportTokenVerifierTest {

    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Test
    void aceptaUnicamenteElTokenConfigurado() {
        BackupReportTokenVerifier verifier = new BackupReportTokenVerifier(TOKEN);

        assertThat(verifier.isValid(TOKEN)).isTrue();
        assertThat(verifier.isValid("0123456789abcdef0123456789abcdee")).isFalse();
        assertThat(verifier.isValid(null)).isFalse();
    }

    @Test
    void unaConfiguracionVaciaNuncaHabilitaElEndpoint() {
        assertThat(new BackupReportTokenVerifier("").isValid("")).isFalse();
    }
}
