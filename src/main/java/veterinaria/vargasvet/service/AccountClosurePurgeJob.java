package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountClosurePurgeJob {

    private final AccountClosureService accountClosureService;

    @Scheduled(cron = "${app.account-closure.purge-cron:0 30 3 * * *}", zone = "${app.reminders.zone:America/Lima}")
    public void purgeExpiredClosures() {
        try {
            int purged;
            do {
                purged = accountClosureService.purgeExpired();
            } while (purged > 0);
        } catch (Exception ex) {
            log.error("No se pudieron eliminar las credenciales de las cuentas cerradas vencidas: {}", ex.getMessage());
        }
    }
}
