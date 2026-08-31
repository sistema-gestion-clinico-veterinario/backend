package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rol_vista_configuracion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rol_id", "vista_id"})
})
public class RolVistaConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Role rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vista_id", nullable = false)
    private Vista vista;

    @Column(nullable = false)
    private Integer orden = 0;
}
