package veterinaria.vargasvet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyCompliancePolicyRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyComplianceServiceActualesTest {

    @Mock private CompanyCompliancePolicyRepository repository;
    @Mock private CompanyRepository companyRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private CompanyComplianceService service;

    @Test
    @DisplayName("Un usuario sin clínica asociada recibe la configuración predeterminada de cada control, sin consultar la base")
    void actualesSinEmpresaDevuelveLosValoresPredeterminados() {
        List<ConfiguracionCumplimientoResponse> respuesta = service.actuales(null);

        assertThat(respuesta).hasSize(ControlCumplimientoClinica.values().length);
        for (ConfiguracionCumplimientoResponse configuracion : respuesta) {
            ControlCumplimientoClinica control = configuracion.control();
            assertThat(configuracion.habilitado()).isEqualTo(control.isHabilitadoPorDefecto());
            assertThat(configuracion.modo()).isEqualTo(control.getModoPorDefecto());
            assertThat(configuracion.configuracionExplicita()).isFalse();
        }
        verifyNoInteractions(companyRepository, repository);
    }

    @Test
    @DisplayName("Una empresa que no existe sigue respondiendo «no encontrada»")
    void actualesConEmpresaInexistenteLanzaNoEncontrada() {
        when(companyRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service.actuales(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Empresa no encontrada");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Una empresa existente sin políticas propias recibe los valores predeterminados de cada control")
    void actualesConEmpresaSinPoliticasDevuelveLosValoresPredeterminados() {
        when(companyRepository.existsById(5)).thenReturn(true);
        when(repository.vigente(eq(5), any(), any())).thenReturn(Optional.empty());

        List<ConfiguracionCumplimientoResponse> respuesta = service.actuales(5);

        assertThat(respuesta).hasSize(ControlCumplimientoClinica.values().length);
        assertThat(respuesta).allSatisfy(configuracion -> assertThat(configuracion.configuracionExplicita()).isFalse());
        verify(repository, times(ControlCumplimientoClinica.values().length)).vigente(eq(5), any(), any());
    }
}
