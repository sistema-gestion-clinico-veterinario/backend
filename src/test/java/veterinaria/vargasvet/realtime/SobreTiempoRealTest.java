package veterinaria.vargasvet.realtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SobreTiempoRealTest {

    @Test
    void unEventoSeRecuperaIgualConAcentosLlavesYComillas() {
        String json = "{\"tipo\":\"CITA_CREADA\",\"nota\":\"Niño \\\"Rex\\\" {a}\\nlínea\",\"total\":12.50}";
        String carga = SobreTiempoReal.evento("origen-1", "/topic/citas/7", json.getBytes(StandardCharsets.UTF_8));

        SobreTiempoReal sobre = SobreTiempoReal.leerEvento(carga).orElseThrow();

        assertThat(sobre.origen()).isEqualTo("origen-1");
        assertThat(sobre.destino()).isEqualTo("/topic/citas/7");
        assertThat(new String(sobre.cuerpo(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    void unEventoNoSeConfundeConUnaSondaNiAlReves() {
        String sonda = SobreTiempoReal.sonda("origen-1");
        String evento = SobreTiempoReal.evento("origen-1", "/topic/caja/1", "{}".getBytes(StandardCharsets.UTF_8));

        assertThat(SobreTiempoReal.leerSonda(sonda)).contains("origen-1");
        assertThat(SobreTiempoReal.leerEvento(sonda)).isEmpty();
        assertThat(SobreTiempoReal.leerSonda(evento)).isEmpty();
    }

    @Test
    void noSeAceptanDestinosFueraDeTopicNiSeparadoresEnLosCampos() {
        byte[] cuerpo = "{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> SobreTiempoReal.evento("o", "/app/citas", cuerpo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobreTiempoReal.evento("o", "/topic/a\nb", cuerpo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobreTiempoReal.evento("o\nx", "/topic/a", cuerpo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobreTiempoReal.evento(" ", "/topic/a", cuerpo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SobreTiempoReal.sonda("a\nb")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lasCargasMalformadasOAjenasSeIgnoranSinFallar() {
        assertThat(SobreTiempoReal.leerEvento(null)).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("")).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("E\nsolo-origen")).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("E\no\n/topic/a")).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("E\no\n/otra/a\n{}")).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("E\n\n/topic/a\n{}")).isEmpty();
        assertThat(SobreTiempoReal.leerEvento("texto cualquiera")).isEmpty();
        assertThat(SobreTiempoReal.leerSonda("S\n")).isEmpty();
        assertThat(SobreTiempoReal.leerSonda(null)).isEmpty();
    }
}
