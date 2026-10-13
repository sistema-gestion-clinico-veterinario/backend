package veterinaria.vargasvet.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiciosVeterinariosTest {

    @Test
    void unTipoNoClinicoNoRequiereConsultaClinicaSinImportarSuNombre() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setNombre("Área de bienestar");
        tipo.setEsVeterinario(false);
        servicio.setTipoEmpleado(tipo);

        assertThat(servicio.requiereConsultaClinica()).isFalse();
    }

    @Test
    void unTipoVeterinarioRequiereConsultaClinicaSinImportarSuNombre() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setNombre("Profesional clínico");
        tipo.setEsVeterinario(true);
        servicio.setTipoEmpleado(tipo);

        assertThat(servicio.requiereConsultaClinica()).isTrue();
    }

    @Test
    void servicioSinTipoConservaFlujoClinicoPorCompatibilidad() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();

        assertThat(servicio.requiereConsultaClinica()).isTrue();
    }
}
