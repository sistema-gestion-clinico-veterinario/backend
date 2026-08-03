package veterinaria.vargasvet.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiciosVeterinariosTest {

    @Test
    void groomingNoRequiereConsultaClinica() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setNombre("Grommer");
        servicio.setTipoEmpleado(tipo);

        assertThat(servicio.requiereConsultaClinica()).isFalse();
    }

    @Test
    void servicioVeterinarioSiRequiereConsultaClinica() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        TipoEmpleado tipo = new TipoEmpleado();
        tipo.setNombre("Veterinario");
        servicio.setTipoEmpleado(tipo);

        assertThat(servicio.requiereConsultaClinica()).isTrue();
    }

    @Test
    void servicioSinTipoConservaFlujoClinicoPorCompatibilidad() {
        ServiciosVeterinarios servicio = new ServiciosVeterinarios();

        assertThat(servicio.requiereConsultaClinica()).isTrue();
    }
}
