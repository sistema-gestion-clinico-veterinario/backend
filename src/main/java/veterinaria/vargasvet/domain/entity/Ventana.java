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

    private String icono;

    private String ruta;

    @Column(name = "sort_order")
    private Integer orden;

    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Ventana parent;

    @ToString.Exclude
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<Ventana> hijos;

    @ToString.Exclude
    @OneToMany(mappedBy = "ventana", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vista> vistas;
}
