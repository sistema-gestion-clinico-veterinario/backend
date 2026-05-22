package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rol_ventana_permisos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rol_id", "ventana_id"})
})
public class RolVentanaPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Role rol;

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
