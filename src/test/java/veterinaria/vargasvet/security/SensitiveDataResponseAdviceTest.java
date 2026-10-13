package veterinaria.vargasvet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;
import veterinaria.vargasvet.dto.response.EmpleadoListResponse;
import veterinaria.vargasvet.dto.response.MascotaRelacionResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveDataResponseAdviceTest {

    @Mock private AccesoValidator accesoValidator;

    private SensitiveDataResponseAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new SensitiveDataResponseAdvice(accesoValidator);
    }

    @Test
    void quienSoloConsultaVeLosDatosParcialmenteOcultos() {
        ApoderadoListResponse cliente = cliente();

        escribir(new ApiResponse<>(true, "ok", new PageImpl<>(List.of(cliente))));

        assertThat(cliente.getEmail()).isEqualTo("a***@clinica.pe");
        assertThat(cliente.getTelefono()).isEqualTo("******321");
        assertThat(cliente.getNumeroDocumento()).isEqualTo("****5678");
        assertThat(cliente.getNombre()).isEqualTo("Ana");
    }

    @Test
    void quienPuedeRegistrarVeLosDatosCompletos() {
        when(accesoValidator.can("VISTA_CLIENTES", "ESCRIBIR")).thenReturn(true);
        ApoderadoListResponse cliente = cliente();

        escribir(new ApiResponse<>(true, "ok", List.of(cliente)));

        assertThat(cliente.getEmail()).isEqualTo("ana@clinica.pe");
        assertThat(cliente.getTelefono()).isEqualTo("987654321");
        assertThat(cliente.getNumeroDocumento()).isEqualTo("12345678");
        verify(accesoValidator, never()).can("VISTA_CLIENTES", "MODIFICAR");
    }

    @Test
    void quienPuedeModificarVeLosDatosCompletos() {
        when(accesoValidator.can("VISTA_CLIENTES", "ESCRIBIR")).thenReturn(false);
        when(accesoValidator.can("VISTA_CLIENTES", "MODIFICAR")).thenReturn(true);
        ApoderadoListResponse cliente = cliente();

        escribir(new ApiResponse<>(true, "ok", cliente));

        assertThat(cliente.getNumeroDocumento()).isEqualTo("12345678");
    }

    @Test
    void cadaTipoDeDatoSeAutorizaConSuPropiaVista() {
        when(accesoValidator.can("VISTA_CLIENTES", "ESCRIBIR")).thenReturn(true);
        EmpleadoListResponse empleado = new EmpleadoListResponse();
        empleado.setEmail("beto@clinica.pe");
        empleado.setTelefono("912345678");

        escribir(new ApiResponse<>(true, "ok", List.of(cliente(), empleado)));

        assertThat(empleado.getEmail()).isEqualTo("b***@clinica.pe");
        assertThat(empleado.getTelefono()).isEqualTo("******678");
        verify(accesoValidator).can("VISTA_EMPLEADOS", "ESCRIBIR");
    }

    @Test
    void laVistaDeUnMismoTipoSeEvaluaUnaSolaVezPorRespuesta() {
        escribir(new ApiResponse<>(true, "ok", List.of(cliente(), cliente(), cliente())));

        verify(accesoValidator, times(1)).can("VISTA_CLIENTES", "ESCRIBIR");
        verify(accesoValidator, times(1)).can("VISTA_CLIENTES", "MODIFICAR");
    }

    @Test
    void elDetalleParaEditarYLasRelacionesTambienSeOcultan() {
        ApoderadoRequest detalle = new ApoderadoRequest();
        detalle.setEmail("ana@clinica.pe");
        detalle.setTelefono("987654321");
        detalle.setNumeroDocumento("12345678");
        MascotaRelacionResponse relacion = new MascotaRelacionResponse();
        relacion.setNumeroDocumento("87654321");

        escribir(new ApiResponse<>(true, "ok", detalle));
        escribir(new ApiResponse<>(true, "ok", List.of(relacion)));

        assertThat(detalle.getEmail()).isEqualTo("a***@clinica.pe");
        assertThat(detalle.getTelefono()).isEqualTo("******321");
        assertThat(detalle.getNumeroDocumento()).isEqualTo("****5678");
        assertThat(relacion.getNumeroDocumento()).isEqualTo("****4321");
    }

    @Test
    void lasRespuestasSinDatosProtegidosNoSeTocan() {
        ApiResponse<String> texto = new ApiResponse<>(true, "ok", "hola");

        assertThat(advice.beforeBodyWrite(texto, null, null, null, null, null)).isSameAs(texto);
        assertThat(advice.beforeBodyWrite(null, null, null, null, null, null)).isNull();
        verify(accesoValidator, never()).can(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private void escribir(Object body) {
        advice.beforeBodyWrite(body, null, null, null, null, null);
    }

    private ApoderadoListResponse cliente() {
        ApoderadoListResponse cliente = new ApoderadoListResponse();
        cliente.setNombre("Ana");
        cliente.setEmail("ana@clinica.pe");
        cliente.setTelefono("987654321");
        cliente.setNumeroDocumento("12345678");
        return cliente;
    }
}
