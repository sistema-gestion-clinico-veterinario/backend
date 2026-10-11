package veterinaria.vargasvet.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogControllerTest {

    private final AuditLogService servicio = mock(AuditLogService.class);
    private final AuditLogRepository repositorio = mock(AuditLogRepository.class);
    private final AuditLogController controlador = new AuditLogController(servicio, repositorio);

    private Page<AuditLog> pagina() {
        return new PageImpl<>(List.of());
    }

    private void comoAdministradorDeClinica(MockedStatic<SecurityUtils> seguridad, Integer empresa) {
        seguridad.when(SecurityUtils::isSuperAdmin).thenReturn(false);
        seguridad.when(SecurityUtils::getCurrentCompanyId).thenReturn(empresa);
        seguridad.when(SecurityUtils::getCurrentUserEmail).thenReturn("ana@patitas.test");
        seguridad.when(SecurityUtils::getCurrentRoleNames).thenReturn(List.of("ROLE_ADMIN"));
    }

    private ResponseEntity<ApiResponse<Page<AuditLog>>> consultar(Integer empresa, String orden, boolean cargaInicial) {
        return controlador.getLogs(empresa, null, null, null, null, null, 0, 20, orden, cargaInicial);
    }

    @Test
    void laConsultaYLaExportacionExigenElPermisoDeAuditoria() throws Exception {
        PreAuthorize regla = AuditLogController.class.getAnnotation(PreAuthorize.class);

        assertThat(regla).isNotNull();
        assertThat(regla.value()).contains("VISTA_AUDITORIA_ADMIN").contains("LEER");
        assertThat(AuditLogController.class.getMethod("exportLogs", Integer.class, String.class, String.class,
                String.class, LocalDateTime.class, LocalDateTime.class)).isNotNull();
    }

    @Test
    void unaClinicaSoloVeSuPropiaEmpresaAunqueIndiqueOtra() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            consultar(999, "timestamp,desc", false);
        }

        verify(servicio).getLogs(eq(7), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void sinEmpresaEnLaSesionSeNiegaElAcceso() {
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, null);

            var respuesta = consultar(null, "timestamp,desc", false);

            assertThat(respuesta.getStatusCode().value()).isEqualTo(403);
        }
        verify(servicio, never()).getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void laPlataformaPuedeElegirLaEmpresa() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            seguridad.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            seguridad.when(SecurityUtils::getCurrentUserEmail).thenReturn("plataforma@vargasvet.test");
            seguridad.when(SecurityUtils::getCurrentRoleNames).thenReturn(List.of("ROLE_SUPER_ADMIN"));

            consultar(3, "timestamp,desc", false);
        }

        verify(servicio).getLogs(eq(3), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void laExportacionTambienQuedaLimitadaALaEmpresaYDejaConstancia() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            controlador.exportLogs(999, null, null, null, null, null);
        }

        verify(servicio).getLogs(eq(7), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        verify(servicio).log(eq("EXPORTAR_AUDITORIA"), eq("Seguridad"), contains("ana@patitas.test"));
    }

    @Test
    void soloSePuedeOrdenarPorCamposDelRegistro() {
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            assertThatThrownBy(() -> consultar(null, "password,desc", false))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("password");
            assertThatThrownBy(() -> consultar(null, "campoQueNoExiste", false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void losCamposPermitidosSeAplicanConSuDireccion() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);
            when(repositorio.existsByUserEmailAndActionAndTimestampAfter(anyString(), anyString(), any()))
                    .thenReturn(true);

            consultar(null, "userEmail,asc", false);
        }

        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).getLogs(any(), any(), any(), any(), any(), any(), paginacion.capture());
        assertThat(paginacion.getValue().getSort().getOrderFor("userEmail").isAscending()).isTrue();
    }

    @Test
    void elTamanoDePaginaTieneUnTopeParaNoVolcarTodoElHistorial() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);
            when(repositorio.existsByUserEmailAndActionAndTimestampAfter(anyString(), anyString(), any()))
                    .thenReturn(true);

            controlador.getLogs(null, null, null, null, null, null, 0, 100000, "timestamp,desc", false);
        }

        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).getLogs(any(), any(), any(), any(), any(), any(), paginacion.capture());
        assertThat(paginacion.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void auditarAlAuditorNoDependeDeLoQueEnvieElCliente() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        when(repositorio.existsByUserEmailAndActionAndTimestampAfter(anyString(), anyString(), any())).thenReturn(false);
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            consultar(null, "timestamp,desc", false);
        }

        verify(servicio).log(eq("ana@patitas.test"), anyString(), eq(7), isNull(),
                eq("CONSULTA_AUDITORIA"), eq("Seguridad"), contains("ana@patitas.test"), isNull());
    }

    @Test
    void dentroDeLaMismaSesionDeConsultaNoSeRepiteElRegistro() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        when(repositorio.existsByUserEmailAndActionAndTimestampAfter(anyString(), anyString(), any())).thenReturn(true);
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            consultar(null, "timestamp,desc", false);
        }

        verify(servicio, never()).log(anyString(), anyString(), anyInt(), any(), eq("CONSULTA_AUDITORIA"),
                anyString(), anyString(), any());
    }

    @Test
    void laCargaInicialSiempreDejaRegistro() {
        when(servicio.getLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(pagina());
        when(repositorio.existsByUserEmailAndActionAndTimestampAfter(anyString(), anyString(), any())).thenReturn(true);
        try (MockedStatic<SecurityUtils> seguridad = mockStatic(SecurityUtils.class)) {
            comoAdministradorDeClinica(seguridad, 7);

            consultar(null, "timestamp,desc", true);
        }

        verify(servicio).log(eq("ana@patitas.test"), anyString(), eq(7), isNull(),
                eq("CONSULTA_AUDITORIA"), eq("Seguridad"), anyString(), isNull());
    }
}
