package veterinaria.vargasvet.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MailDelivery {

    private static final long WAIT_SECONDS = 30;

    private MailDelivery() {
    }

    public static boolean failed(CompletableFuture<Boolean> sending) {
        if (sending == null) {
            return false;
        }
        try {
            return !Boolean.TRUE.equals(sending.get(WAIT_SECONDS, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            return true;
        }
    }
}
