package veterinaria.vargasvet.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class TiempoRealPruebas {

    private TiempoRealPruebas() {
    }

    static final class CanalCapturador implements MessageChannel {

        final List<Message<?>> mensajes = new CopyOnWriteArrayList<>();

        @Override
        public boolean send(Message<?> mensaje) {
            mensajes.add(mensaje);
            return true;
        }

        @Override
        public boolean send(Message<?> mensaje, long tiempo) {
            return send(mensaje);
        }
    }

    static MessageConverter conversor() {
        ObjectMapper mapeador = new ObjectMapper();
        mapeador.findAndRegisterModules();
        MappingJackson2MessageConverter json = new MappingJackson2MessageConverter();
        json.setObjectMapper(mapeador);
        return new CompositeMessageConverter(List.of(new StringMessageConverter(), new ByteArrayMessageConverter(), json));
    }

    static SimpMessagingTemplate plantilla(CanalCapturador canal) {
        SimpMessagingTemplate plantilla = new SimpMessagingTemplate(canal);
        plantilla.setMessageConverter(conversor());
        return plantilla;
    }
}
