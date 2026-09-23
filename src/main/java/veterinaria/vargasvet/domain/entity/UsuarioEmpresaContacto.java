package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Dato de contacto (telefono, direccion) de un usuario para UNA empresa puntual (company
 * null = SuperAdmin/global, sin empresa). Antes Usuario.telefono/direccion eran un solo
 * valor global compartido entre todas las relaciones empresariales de la persona - mismo
 * problema que tenia la contraseña antes de UsuarioEmpresaCredencial, y misma solución. */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario_empresa_contacto")
public class UsuarioEmpresaContacto {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Null solo para el contacto "global" de un SuperAdmin (sin empresa asociada). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column
    private String telefono;

    @Column
    private String direccion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
