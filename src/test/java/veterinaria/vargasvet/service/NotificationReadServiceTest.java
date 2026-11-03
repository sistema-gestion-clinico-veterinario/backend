package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.repository.NotificationReadStateRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationReadServiceTest {

    private final NotificationReadStateRepository repository = mock(NotificationReadStateRepository.class);
    private final NotificationReadService service = new NotificationReadService(repository);

    @Test
    void devuelveLasClavesLeidasSinDuplicados() {
        when(repository.findKeysByUsuarioId(17))
                .thenReturn(List.of("respaldo-20", "respaldo-20", "ia"));

        assertThat(service.list(17)).containsExactly("respaldo-20", "ia");
    }

    @Test
    void registraLaLecturaParaElUsuarioActual() {
        service.markRead(17, "respaldo-20");

        verify(repository).insertIfAbsent(eq(17), eq("respaldo-20"), isA(Instant.class));
    }
}
