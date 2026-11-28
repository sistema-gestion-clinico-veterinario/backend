package veterinaria.vargasvet.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class PublicadorTiempoReal {

    private final SimpMessagingTemplate mensajeria;
    private final Optional<DifusionEntreInstancias> difusion;

    public PublicadorTiempoReal(SimpMessagingTemplate mensajeria, Optional<DifusionEntreInstancias> difusion) {
        this.mensajeria = mensajeria;
        this.difusion = difusion;
    }

    public void publicar(String destino, Object cuerpo) {
        mensajeria.convertAndSend(destino, cuerpo);
        difusion.ifPresent(canal -> difundir(canal, destino, cuerpo));
    }

    private void difundir(DifusionEntreInstancias canal, String destino, Object cuerpo) {
        try {
            canal.difundir(destino, cuerpo);
        } catch (Exception e) {
            log.warn("No se pudo difundir el evento de tiempo real a las demás instancias: {}", e.getClass().getSimpleName());
        }
    }
}
