package veterinaria.vargasvet.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardOverviewPrivacyTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void dashboardOverviewDoesNotExposeContactIdentityOrTechnicalAuditData() throws Exception {
        DashboardOverviewDTO overview = DashboardOverviewDTO.builder()
                .recentLogs(List.of(DashboardAuditSummary.builder()
                        .id(1L)
                        .timestamp(LocalDateTime.of(2026, 10, 11, 10, 30))
                        .userRole("ADMIN")
                        .action("ACTUALIZAR_CLIENTE")
                        .module("CLIENTES")
                        .build()))
                .employees(List.of(DashboardEmployeeSummary.builder()
                        .id(2L)
                        .nombre("Ana")
                        .apellido("Torres")
                        .activo(true)
                        .build()))
                .guardians(List.of(DashboardGuardianSummary.builder()
                        .id(3L)
                        .nombre("Luis")
                        .apellido("Paredes")
                        .activo(true)
                        .build()))
                .build();

        String json = objectMapper.writeValueAsString(overview);

        assertThat(json)
                .contains("\"recentLogs\"", "\"employees\"", "\"guardians\"")
                .doesNotContain(
                        "\"email\"",
                        "\"telefono\"",
                        "\"numeroDocumento\"",
                        "\"direccion\"",
                        "\"ipAddress\"",
                        "\"dispositivo\"",
                        "\"details\"",
                        "\"clienteNombre\"",
                        "\"apoderadoNombreCompleto\"");
    }
}
