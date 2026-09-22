package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Credencial de acceso por contraseña de un usuario para UNA empresa puntual
 * (company null = SuperAdmin, sin empresa). La contraseña pertenece a la relación
 * usuario+empresa, no al usuario global - dos empresas nunca comparten hash ni
 * versión de credenciales, aunque la persona escriba la misma contraseña en ambas. */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario_empresa_credencial")
public class UsuarioEmpresaCredencial {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Null solo para la credencial de un SuperAdmin (sin empresa asociada). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String password;

    @Column(name = "password_changed", nullable = false)
    private boolean passwordChanged = false;

    @Column(name = "credentials_version", nullable = false)
    private long credentialsVersion = 0L;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
