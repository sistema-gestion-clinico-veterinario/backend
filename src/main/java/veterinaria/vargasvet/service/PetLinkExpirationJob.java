package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetLinkExpirationJob {

    private final MascotaRelacionService mascotaRelacionService;

    @Scheduled(cron = "${app.pet-link.expiration-cron:0 50 3 * * *}", zone = "${app.reminders.zone:America/Lima}")
    public void cerrarVencidas() {
        try {
            int cerradas;
            do {
                cerradas = mascotaRelacionService.cerrarVencidas();
            } while (cerradas > 0);
        } catch (Exception ex) {
            log.error("No se pudieron cerrar los vínculos con mascotas vencidos: {}", ex.getMessage());
        }
    }
}
