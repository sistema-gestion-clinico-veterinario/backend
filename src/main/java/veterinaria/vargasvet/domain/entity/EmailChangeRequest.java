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

    /** La empresa desde la que se solicitó el cambio (resuelta por la sesión activa al
     * momento del pedido, no Usuario.company - ese campo es solo una cache legacy que
     * puede estar vacía o desactualizada). Se usa para armar el slug del enlace de
     * confirmación del correo - sin esto, el enlace podía aterrizar sin slug y dejar a
     * la persona sin poder iniciar sesión después de confirmar. Null = SuperAdmin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

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
