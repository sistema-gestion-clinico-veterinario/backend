package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.NotificationReadState;

import java.util.List;
import java.time.Instant;

public interface NotificationReadStateRepository extends JpaRepository<NotificationReadState, Long> {

    @Query("select state.notificationKey from NotificationReadState state where state.usuarioId = :usuarioId")
    List<String> findKeysByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query(value = """
            INSERT INTO notification_read_state (usuario_id, notification_key, read_at)
            VALUES (:usuarioId, :notificationKey, :readAt)
            ON CONFLICT (usuario_id, notification_key) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("usuarioId") Integer usuarioId,
                        @Param("notificationKey") String notificationKey,
                        @Param("readAt") Instant readAt);
}
