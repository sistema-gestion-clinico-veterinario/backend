package veterinaria.vargasvet.realtime;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

import java.util.function.Consumer;

public class EntregaLocalTiempoReal implements Consumer<SobreTiempoReal> {

    private final SimpMessagingTemplate mensajeria;

    public EntregaLocalTiempoReal(SimpMessagingTemplate mensajeria) {
        this.mensajeria = mensajeria;
    }

    @Override
    public void accept(SobreTiempoReal sobre) {
        Message<byte[]> mensaje = MessageBuilder.withPayload(sobre.cuerpo())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build();
        mensajeria.send(sobre.destino(), mensaje);
    }
}
