package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoOAuthVariasInstanciasTest {

    private static final int INSTANCIAS = 5;

    private final RepositorioEstadoOAuthEnMemoria baseCompartida = new RepositorioEstadoOAuthEnMemoria();

    private EstadoOAuthCompartido instancia(int semillaDeClave) {
        byte[] clave = new byte[32];
        Arrays.fill(clave, (byte) semillaDeClave);
        return new EstadoOAuthCompartido(baseCompartida, new CifradoEstadoOAuth(clave), new ObjectMapper());
    }

    private List<GoogleOAuthFlowStore> flujos() {
        List<GoogleOAuthFlowStore> lista = new ArrayList<>();
        for (int i = 0; i < INSTANCIAS; i++) {
            lista.add(new GoogleOAuthFlowStore(instancia(1)));
        }
        return lista;
    }

    @Test
    void elFlujoCompletoFuncionaAunqueCadaPasoCaigaEnOtraInstancia() {
        List<GoogleOAuthFlowStore> flujos = flujos();
        List<GoogleLoginExchangeStore> canjes = new ArrayList<>();
        for (int i = 0; i < INSTANCIAS; i++) {
            canjes.add(new GoogleLoginExchangeStore(instancia(1)));
        }

        String intent = flujos.get(0).createIntent(new GoogleOAuthFlowStore.Context("vargas-vet", null));
        GoogleOAuthFlowStore.Context contexto = flujos.get(1).consumeIntent(intent);
        String state = flujos.get(2).createAuthorization(new GoogleOAuthFlowStore.Authorization(contexto, "verificador"));
        GoogleOAuthFlowStore.Authorization autorizacion = flujos.get(3).consumeAuthorization(state);
        AuthResponse sesion = new AuthResponse();
        sesion.setToken("t");
        sesion.setRefreshToken("r");
        String codigo = canjes.get(3).store(sesion);
        AuthResponse canjeada = canjes.get(4).consume(codigo);

        assertThat(contexto.slug()).isEqualTo("vargas-vet");
        assertThat(autorizacion.codeVerifier()).isEqualTo("verificador");
        assertThat(canjeada.getToken()).isEqualTo("t");
        assertThat(canjeada.getRefreshToken()).isEqualTo("r");
    }

    @Test
    void unCodigoSeCanjeaExactamenteUnaVezAunqueTodasLasInstanciasLoIntentenALaVez() throws Exception {
        List<GoogleOAuthFlowStore> flujos = flujos();
        String intent = flujos.get(0).createIntent(new GoogleOAuthFlowStore.Context("vargas-vet", null));
        int hilos = 40;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<GoogleOAuthFlowStore.Context>> intentos = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            GoogleOAuthFlowStore instancia = flujos.get(i % INSTANCIAS);
            intentos.add(pool.submit(() -> {
                salida.await();
                return instancia.consumeIntent(intent);
            }));
        }
        salida.countDown();
        int exitos = 0;
        for (Future<GoogleOAuthFlowStore.Context> intento : intentos) {
            if (intento.get(10, TimeUnit.SECONDS) != null) {
                exitos++;
            }
        }
        pool.shutdownNow();

        assertThat(exitos).isEqualTo(1);
    }

    @Test
    void unaInstanciaConOtraClaveNoPuedeLeerLoQueGuardaronLasDemasYLoDescarta() {
        GoogleOAuthFlowStore correcta = new GoogleOAuthFlowStore(instancia(1));
        GoogleOAuthFlowStore desalineada = new GoogleOAuthFlowStore(instancia(2));
        String intent = correcta.createIntent(new GoogleOAuthFlowStore.Context("vargas-vet", null));

        assertThat(desalineada.consumeIntent(intent)).isNull();
        assertThat(correcta.consumeIntent(intent)).isNull();
    }
}
