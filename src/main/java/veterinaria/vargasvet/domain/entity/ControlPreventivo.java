package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "control_preventivo", indexes = {
        @Index(name = "idx_control_preventivo_fecha_estado", columnList = "fecha_recomendada,estado"),
        @Index(name = "idx_control_preventivo_mascota", columnList = "mascota_id")
})
public class ControlPreventivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    @ToString.Exclude
    private Mascota mascota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoControlPreventivo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vacuna_id")
    @ToString.Exclude
    private TipoVacuna tipoVacuna;

    @Column(name = "nombre_control", nullable = false, length = 100)
    private String nombreControl;

    @Column(name = "fecha_recomendada", nullable = false)
    private LocalDate fechaRecomendada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoControlPreventivo estado = EstadoControlPreventivo.PROGRAMADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_suspende_id")
    @ToString.Exclude
    private Cita citaSuspende;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 150)
    private String createdBy;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;
    @Column(name = "estado_modificado_por", length = 150)
    private String estadoModificadoPor;
    @Column(name = "fecha_modificacion_estado")
    private LocalDateTime fechaModificacionEstado;

    @PrePersist
    void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = veterinaria.vargasvet.util.AppClock.now();
    }
}
