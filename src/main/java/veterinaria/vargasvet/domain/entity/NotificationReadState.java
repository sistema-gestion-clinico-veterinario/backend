package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_read_state", uniqueConstraints =
        @UniqueConstraint(name = "uq_notification_read_state_user_key",
                columnNames = {"usuario_id", "notification_key"}))
public class NotificationReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "notification_key", nullable = false, length = 120)
    private String notificationKey;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;
}
