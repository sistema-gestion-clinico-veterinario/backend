package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoRecordatorio;
import veterinaria.vargasvet.domain.enums.TipoAvisoRecordatorio;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recordatorio_preventivo", uniqueConstraints = @UniqueConstraint(
        name = "uk_recordatorio_control_tipo", columnNames = {"control_preventivo_id", "tipo_aviso"}))
public class RecordatorioPreventivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_preventivo_id", nullable = false)
    private ControlPreventivo controlPreventivo;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_aviso", nullable = false, length = 20)
    private TipoAvisoRecordatorio tipoAviso;
    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;
    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecordatorio estado;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 150)
    private String createdBy;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;

    @PrePersist
    void onCreate() { createdAt = veterinaria.vargasvet.util.AppClock.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = veterinaria.vargasvet.util.AppClock.now(); }
}
