package veterinaria.vargasvet.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import veterinaria.vargasvet.domain.entity.AuditLog;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditLogRepositoryConsultaTest {

    @Autowired private AuditLogRepository repositorio;

    private void registrar(String correo, String accion, LocalDateTime momento) {
        repositorio.save(AuditLog.builder().timestamp(momento).userEmail(correo).action(accion)
                .module("Seguridad").details("detalle").build());
    }

    @Test
    void detectaUnaConsultaRecienteDeEsaPersona() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 11, 12, 0);
        registrar("ana@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(3));

        assertThat(repositorio.existsByUserEmailAndActionAndTimestampAfter(
                "ana@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(10))).isTrue();
    }

    @Test
    void unaConsultaAntiguaNoCuentaParaLaVentana() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 11, 12, 0);
        registrar("ana@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(30));

        assertThat(repositorio.existsByUserEmailAndActionAndTimestampAfter(
                "ana@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(10))).isFalse();
    }

    @Test
    void laConsultaDeOtraPersonaOOtraAccionNoCuenta() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 11, 12, 0);
        registrar("beto@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(1));
        registrar("ana@patitas.test", "LOGIN_EXITOSO", ahora.minusMinutes(1));

        assertThat(repositorio.existsByUserEmailAndActionAndTimestampAfter(
                "ana@patitas.test", "CONSULTA_AUDITORIA", ahora.minusMinutes(10))).isFalse();
    }
}
