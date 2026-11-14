package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.service.impl.AuditLogServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

class AuditoriaEmpresaPorSolicitudTest {

    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final AuditLogServiceImpl servicio = new AuditLogServiceImpl(auditLogRepository,
            mock(AuditRealtimePublisher.class), companyRepository, mock(ClientIpResolver.class));

    @AfterEach
    void cerrarLaSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void variosEventosDeUnaMismaSolicitudConsultanLaEmpresaUnaSolaVez() {
        abrirSolicitud();
        when(companyRepository.findById(7)).thenReturn(Optional.of(empresa(7, "Clinica Patitas")));

        servicio.log(7, "PRIMERO", "Horarios", "uno");
        servicio.log(7, "SEGUNDO", "Horarios", "dos");
        servicio.logBusiness(7, "TERCERO", "Horarios", "tres", null, null, null, null, null);

        verify(companyRepository, times(1)).findById(7);
        assertThat(nombresGuardados()).containsOnly("Clinica Patitas").hasSize(3);
    }

    @Test
    void cadaSolicitudNuevaVuelveAConsultarLaEmpresaYVeElNombreActual() {
        when(companyRepository.findById(7)).thenReturn(Optional.of(empresa(7, "Nombre viejo")));
        abrirSolicitud();
        servicio.log(7, "PRIMERO", "Horarios", "uno");

        when(companyRepository.findById(7)).thenReturn(Optional.of(empresa(7, "Nombre nuevo")));
        abrirSolicitud();
        servicio.log(7, "SEGUNDO", "Horarios", "dos");

        verify(companyRepository, times(2)).findById(7);
        assertThat(nombresGuardados()).containsExactly("Nombre viejo", "Nombre nuevo");
    }

    @Test
    void empresasDistintasDeLaMismaSolicitudNoSeMezclan() {
        abrirSolicitud();
        when(companyRepository.findById(7)).thenReturn(Optional.of(empresa(7, "Clinica Siete")));
        when(companyRepository.findById(8)).thenReturn(Optional.of(empresa(8, "Clinica Ocho")));

        servicio.log(7, "A", "Horarios", "uno");
        servicio.log(8, "B", "Horarios", "dos");
        servicio.log(7, "C", "Horarios", "tres");

        verify(companyRepository, times(1)).findById(7);
        verify(companyRepository, times(1)).findById(8);
        assertThat(nombresGuardados()).containsExactly("Clinica Siete", "Clinica Ocho", "Clinica Siete");
    }

    @Test
    void unaEmpresaInexistenteTambienSeConsultaUnaSolaVezPorSolicitud() {
        abrirSolicitud();
        when(companyRepository.findById(9)).thenReturn(Optional.empty());

        servicio.log(9, "A", "Horarios", "uno");
        servicio.log(9, "B", "Horarios", "dos");

        verify(companyRepository, times(1)).findById(9);
        assertThat(nombresGuardados()).containsOnlyNulls();
    }

    @Test
    void unaConsultaFallidaNoSeRecuerdaYElEventoSeGuardaSinNombre() {
        abrirSolicitud();
        when(companyRepository.findById(7)).thenThrow(new IllegalStateException("base no disponible"))
                .thenReturn(Optional.of(empresa(7, "Clinica Patitas")));

        servicio.log(7, "A", "Horarios", "uno");
        servicio.log(7, "B", "Horarios", "dos");

        verify(companyRepository, times(2)).findById(7);
        assertThat(nombresGuardados()).containsExactly(null, "Clinica Patitas");
    }

    @Test
    void fueraDeUnaSolicitudCadaEventoConsultaLaEmpresa() {
        when(companyRepository.findById(7)).thenReturn(Optional.of(empresa(7, "Clinica Patitas")));

        servicio.log("ana@clinica.test", "ROLE_ADMIN", 7, "Clinica Patitas", "A", "Horarios", "uno", null);
        servicio.log(7, "B", "Horarios", "dos");
        servicio.log(7, "C", "Horarios", "tres");

        verify(companyRepository, times(2)).findById(7);
    }

    @Test
    void sinEmpresaNoConsultaNada() {
        abrirSolicitud();

        servicio.log(null, "A", "Horarios", "uno");

        verify(companyRepository, never()).findById(any());
    }

    private void abrirSolicitud() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private Company empresa(Integer id, String nombre) {
        Company company = new Company();
        company.setId(id);
        company.setName(nombre);
        return company;
    }

    private java.util.List<String> nombresGuardados() {
        var captor = forClass(AuditLog.class);
        verify(auditLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(AuditLog::getCompanyName).toList();
    }
}
