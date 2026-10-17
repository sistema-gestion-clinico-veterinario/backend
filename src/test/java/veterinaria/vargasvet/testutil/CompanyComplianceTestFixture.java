package veterinaria.vargasvet.testutil;

import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;
import veterinaria.vargasvet.service.CompanyComplianceService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class CompanyComplianceTestFixture {
    private CompanyComplianceTestFixture() { }

    public static CompanyComplianceService controlesSoftvetHabilitados() {
        CompanyComplianceService service = mock(CompanyComplianceService.class);
        when(service.gestionadoPorSoftvet(anyInt(), any())).thenReturn(true);
        when(service.resolver(anyInt(), any())).thenAnswer(invocation -> {
            ControlCumplimientoClinica control = invocation.getArgument(1);
            return new ConfiguracionCumplimientoResponse(control, 1, true, ModoGestionCumplimiento.SOFTVET,
                    control == ControlCumplimientoClinica.USO_IA, "Configuración de prueba", null,
                    null, null, "Pruebas", true);
        });
        return service;
    }

    /** Fixture mínimo para servicios que solo consultan si SoftVet gestiona un control. */
    public static CompanyComplianceService controlesGestionadosPorSoftvet() {
        CompanyComplianceService service = mock(CompanyComplianceService.class);
        when(service.gestionadoPorSoftvet(anyInt(), any())).thenReturn(true);
        return service;
    }
}
