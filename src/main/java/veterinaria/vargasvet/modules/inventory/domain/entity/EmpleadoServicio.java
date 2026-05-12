package veterinaria.vargasvet.modules.inventory.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.entity.Empleado;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "empleado_servicio")
public class EmpleadoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false, referencedColumnName = "id")
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false, referencedColumnName = "id")
    private ServiciosVeterinarios servicio;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    public EmpleadoServicio() {
    }

    public EmpleadoServicio(Empleado empleado, ServiciosVeterinarios servicio) {
        this.empleado = empleado;
        this.servicio = servicio;
        this.fechaAsignacion = LocalDateTime.now();
    }
}
