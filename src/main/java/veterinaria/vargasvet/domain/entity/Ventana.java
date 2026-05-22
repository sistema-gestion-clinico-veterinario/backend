package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "ventanas")
public class Ventana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "grupo", nullable = true)
    private String grupo;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "ventana", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Vista> vistas;
}
