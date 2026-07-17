package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import veterinaria.vargasvet.domain.enums.TipoControlServicio;

@Data
@Entity
@Table(name = "servicios")
public class ServiciosVeterinarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean disponible = true;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "duracion_estimada")
    private Integer duracionEstimada;

    @Column(name = "permite_emergencia", nullable = false, columnDefinition = "boolean default false")
    private Boolean permiteEmergencia = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_control_preventivo", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'NO_APLICA'")
    private TipoControlServicio tipoControlPreventivo = TipoControlServicio.NO_APLICA;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL)
    private List<EmpleadoServicio> empleados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_empleado_id", nullable = true)
    private TipoEmpleado tipoEmpleado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
