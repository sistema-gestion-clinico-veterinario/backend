package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "empleado_servicio")
public class EmpleadoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_veterinario_id", nullable = false, referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_empleado_servicio_empleado_veterinario"))
    private EmpleadoVeterinario empleadoVeterinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false, referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_empleado_servicio_servicio"))
    private ServiciosVeterinarios servicio;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    public EmpleadoServicio() {
    }

    public EmpleadoServicio(EmpleadoVeterinario empleadoVeterinario, ServiciosVeterinarios servicio) {
        this.empleadoVeterinario = empleadoVeterinario;
        this.servicio = servicio;
        this.fechaAsignacion = LocalDateTime.now();
    }
}
