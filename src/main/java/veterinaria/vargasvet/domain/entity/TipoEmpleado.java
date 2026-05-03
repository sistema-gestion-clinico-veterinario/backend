package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tipo_empleado")
public class TipoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;
    @Column(nullable = false)
    private Boolean estado = true;
    @Column(name = "permite_especialidades", nullable = false)
    private Boolean permiteEspecialidades = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
