package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registro_vacunas")
public class RegistroVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = true)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinario_id")
    private Empleado veterinario;

    @Column(name = "nombre_vacuna", nullable = false, length = 200)
    private String nombreVacuna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vacuna_id")
    private TipoVacuna tipoVacuna;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_preventivo_id")
    private ControlPreventivo controlPreventivo;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "fecha_proxima_dosis")
    private LocalDate fechaProximaDosis;

    @Column(name = "periodicidad_meses")
    private Integer periodicidadMeses;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "created_by", updatable = false, length = 150, columnDefinition = "varchar(150) default 'SYSTEM'")
    private String createdBy;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 150, columnDefinition = "varchar(150) default 'SYSTEM'")
    private String updatedBy;
    @Column(name = "estado_modificado_por", length = 150)
    private String estadoModificadoPor;
    @Column(name = "fecha_modificacion_estado")
    private LocalDateTime fechaModificacionEstado;

    @PrePersist
    protected void onCreate() {
        createdAt = veterinaria.vargasvet.util.AppClock.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = veterinaria.vargasvet.util.AppClock.now(); }
}
