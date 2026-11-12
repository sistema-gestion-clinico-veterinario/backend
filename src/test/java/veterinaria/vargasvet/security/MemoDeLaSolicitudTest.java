package veterinaria.vargasvet.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MemoDeLaSolicitudTest {

    private final AtomicInteger calculos = new AtomicInteger();

    @AfterEach
    void cerrarLaSolicitud() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void dentroDeUnaSolicitudCalculaElValorUnaSolaVezPorClave() {
        abrirSolicitud();

        String primero = MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);
        String segundo = MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);

        assertThat(primero).isEqualTo("valor-1");
        assertThat(segundo).isEqualTo("valor-1");
        assertThat(calculos).hasValue(1);
    }

    @Test
    void clavesDistintasNoSeMezclan() {
        abrirSolicitud();

        String a = MemoDeLaSolicitud.obtener("a", String.class, this::calcular);
        String b = MemoDeLaSolicitud.obtener("b", String.class, this::calcular);

        assertThat(a).isEqualTo("valor-1");
        assertThat(b).isEqualTo("valor-2");
    }

    @Test
    void cadaSolicitudNuevaVuelveACalcular() {
        abrirSolicitud();
        MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);

        abrirSolicitud();
        String deLaSegundaSolicitud = MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);

        assertThat(deLaSegundaSolicitud).isEqualTo("valor-2");
    }

    @Test
    void fueraDeUnaSolicitudCalculaCadaVezYNoGuardaNada() {
        MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);
        MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);

        assertThat(calculos).hasValue(2);
        assertThat(MemoDeLaSolicitud.hayUnaSolicitudActiva()).isFalse();
    }

    @Test
    void siElValorGuardadoEsDeOtroTipoLoRecalcula() {
        MockHttpServletRequest solicitud = abrirSolicitud();
        solicitud.setAttribute("clave", 42);

        String valor = MemoDeLaSolicitud.obtener("clave", String.class, this::calcular);

        assertThat(valor).isEqualTo("valor-1");
    }

    @Test
    void reconoceCuandoHayUnaSolicitudActiva() {
        assertThat(MemoDeLaSolicitud.hayUnaSolicitudActiva()).isFalse();

        abrirSolicitud();

        assertThat(MemoDeLaSolicitud.hayUnaSolicitudActiva()).isTrue();
    }

    private MockHttpServletRequest abrirSolicitud() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(solicitud));
        return solicitud;
    }

    private String calcular() {
        return "valor-" + calculos.incrementAndGet();
    }
}
