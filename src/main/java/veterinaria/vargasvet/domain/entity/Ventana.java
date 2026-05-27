package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "El codigo de la ventana es obligatorio")
    @Size(min = 3, max = 80, message = "El codigo debe tener entre 3 y 80 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_\\s-]+$", message = "El codigo solo puede contener letras, numeros, guiones y espacios")
    private String codigo;

    @Column(nullable = false)
    @NotBlank(message = "El nombre de la ventana es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @Column(name = "grupo", nullable = true)
    @Size(max = 40, message = "El grupo no debe superar 40 caracteres")
    @Pattern(regexp = "^$|^[A-Za-z0-9_\\s-]+$", message = "El grupo solo puede contener letras, numeros, guiones y espacios")
    private String grupo;

    @Column(nullable = false)
    @Min(value = 0, message = "El orden no puede ser negativo")
    private Integer orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "ventana", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Vista> vistas;
}
