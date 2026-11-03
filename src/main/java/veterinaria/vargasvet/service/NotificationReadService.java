package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.repository.NotificationReadStateRepository;
import veterinaria.vargasvet.util.AppClock;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationReadService {

    private final NotificationReadStateRepository repository;

    @Transactional(readOnly = true)
    public Set<String> list(Integer usuarioId) {
        return new LinkedHashSet<>(repository.findKeysByUsuarioId(usuarioId));
    }

    @Transactional
    public void markRead(Integer usuarioId, String notificationKey) {
        repository.insertIfAbsent(usuarioId, notificationKey, AppClock.instantNow());
    }
}
