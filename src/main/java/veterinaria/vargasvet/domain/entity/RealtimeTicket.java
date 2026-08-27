package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "realtime_tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeTicket {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String jti;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
