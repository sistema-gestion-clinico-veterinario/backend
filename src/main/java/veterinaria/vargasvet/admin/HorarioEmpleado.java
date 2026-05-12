package veterinaria.vargasvet.admin;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.shared.DiaSemana;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "horario_empleado",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empleado_id", "dia_semana"}))
public class HorarioEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
