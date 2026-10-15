package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;
import veterinaria.vargasvet.dto.request.ConfigurarCumplimientoClinicaRequest;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyComplianceServiceTest {

    private final CompanyComplianceService service = new CompanyComplianceService(null, null, null, null);

    @Test
    void aplicaValoresSegurosCuandoAunNoExisteConfiguracionExplicita() {
        ConfiguracionCumplimientoResponse ia = service.resolver(null, ControlCumplimientoClinica.USO_IA);
        ConfiguracionCumplimientoResponse veterinario = service.resolver(
                null, ControlCumplimientoClinica.CONSENTIMIENTO_VETERINARIO);

        assertThat(ia.habilitado()).isTrue();
        assertThat(ia.modo()).isEqualTo(ModoGestionCumplimiento.SOFTVET);
        assertThat(ia.bloqueante()).isTrue();
        assertThat(ia.configuracionExplicita()).isFalse();
        assertThat(veterinario.habilitado()).isFalse();
        assertThat(veterinario.modo()).isEqualTo(ModoGestionCumplimiento.NO_APLICA);
    }

    @Test
    void rechazaUnControlDeshabilitadoQuePretendeBloquear() {
        ConfigurarCumplimientoClinicaRequest request = new ConfigurarCumplimientoClinicaRequest(
                false, ModoGestionCumplimiento.NO_APLICA, true,
                "No forma parte del servicio contratado", null, true, null);

        assertThatThrownBy(() -> service.configurar(1, ControlCumplimientoClinica.USO_IA, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser bloqueante");
    }

    @Test
    void rechazaGestionExternaComoBloqueoInterno() {
        ConfigurarCumplimientoClinicaRequest request = new ConfigurarCumplimientoClinicaRequest(
                true, ModoGestionCumplimiento.EXTERNO, true,
                "La clínica conserva evidencia fuera de SoftVet", "Contrato 2026", true, null);

        assertThatThrownBy(() -> service.configurar(1, ControlCumplimientoClinica.USO_IA, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gestiona externamente");
    }

    @Test
    void exigeRevisionLegalAntesDeCrearUnaVersion() {
        ConfigurarCumplimientoClinicaRequest request = new ConfigurarCumplimientoClinicaRequest(
                true, ModoGestionCumplimiento.SOFTVET, false,
                "Configuración revisada para esta clínica", null, false, null);

        assertThatThrownBy(() -> service.configurar(1, ControlCumplimientoClinica.DERECHOS_DATOS, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revisión legal");
    }
}
