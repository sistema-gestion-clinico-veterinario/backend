package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registro_desparasitaciones")
public class RegistroDesparasitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Empleado veterinario;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_preventivo_id")
    private ControlPreventivo controlPreventivo;
    @Column(nullable = false, length = 100)
    private String producto;
    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;
    @Column(name = "periodicidad_meses", nullable = false)
    private Integer periodicidadMeses;
    @Column(name = "fecha_proxima_aplicacion", nullable = false)
    private LocalDate fechaProximaAplicacion;
    @Column(nullable = false)
    private Boolean activo = true;
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
    void onCreate() { createdAt = veterinaria.vargasvet.util.AppClock.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = veterinaria.vargasvet.util.AppClock.now(); }
}
