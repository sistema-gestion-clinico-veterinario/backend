package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetAuthorizationReviewJob {

    private static final int BATCH_SIZE = 200;

    private final PetOwnershipService petOwnershipService;

    @Scheduled(cron = "${app.pet-authorization.review-cron:0 45 3 * * *}", zone = "${app.reminders.zone:America/Lima}")
    public void reviewPets() {
        try {
            long cursor = 0;
            while (cursor >= 0) {
                cursor = petOwnershipService.reviewInactiveOwners(cursor, BATCH_SIZE);
            }
        } catch (Exception ex) {
            log.error("No se pudo revisar las mascotas sin una persona activa que las autorice: {}", ex.getMessage());
        }
    }
}
