package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MenuPresentation;

@Data
@Entity
@Table(name = "rol_ventana_configuracion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rol_id", "ventana_id"})
})
public class RolVentanaConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Role rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ventana_id", nullable = false)
    private Ventana ventana;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MenuPresentation presentacion = MenuPresentation.GROUPED;

    @Column(nullable = false)
    private Integer orden = 0;
}
