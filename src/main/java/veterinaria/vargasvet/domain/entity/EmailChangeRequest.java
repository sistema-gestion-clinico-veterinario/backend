package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_change_requests")
@Getter
@Setter
@NoArgsConstructor
public class EmailChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "new_email", nullable = false, length = 254)
    private String newEmail;

    @Column(name = "old_email_token_hash", nullable = false, unique = true, length = 64)
    private String oldEmailTokenHash;

    @Column(name = "new_email_token_hash", nullable = false, unique = true, length = 64)
    private String newEmailTokenHash;

    @Column(name = "old_email_confirmed_at")
    private LocalDateTime oldEmailConfirmedAt;

    @Column(name = "new_email_confirmed_at")
    private LocalDateTime newEmailConfirmedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
