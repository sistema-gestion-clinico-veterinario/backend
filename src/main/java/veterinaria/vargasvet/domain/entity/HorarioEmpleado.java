package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "horario_empleado", uniqueConstraints = {
    @UniqueConstraint(name = "uq_horario_empleado_fecha_hora", columnNames = {"empleado_id", "fecha", "hora_inicio"})
})
public class HorarioEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha", nullable = true)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Campos de Auditoría
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
