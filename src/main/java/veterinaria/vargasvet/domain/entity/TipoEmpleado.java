package veterinaria.vargasvet.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.validation.MeaningfulText;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "tipo_empleado")
public class TipoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "nombre", nullable = false)
    @NotBlank(message = "El nombre del tipo de empleado es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s()\\-]+$", message = "El nombre solo debe contener letras, espacios, parentesis o guiones")
    private String nombre;

    @Column(name = "descripcion")
    @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
    @MeaningfulText(message = "La descripcion debe contener texto real, no solo numeros o simbolos")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private Boolean estado = true;

    @Column(name = "permite_especialidades", nullable = false)
    private Boolean permiteEspecialidades = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
