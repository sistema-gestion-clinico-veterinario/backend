package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario_por_rol_permisos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_por_rol_id", "ventana_id"})
})
public class UsuarioPorRolPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_por_rol_id", nullable = false)
    private UsuarioPorRol usuarioPorRol;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ventana_id", nullable = false)
    private Ventana ventana;

    @Column(nullable = false)
    private boolean leer = false;

    @Column(nullable = false)
    private boolean escribir = false;

    @Column(nullable = false)
    private boolean modificar = false;

    @Column(nullable = false)
    private boolean eliminar = false;
}
