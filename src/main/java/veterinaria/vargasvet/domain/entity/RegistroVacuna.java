package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import veterinaria.vargasvet.domain.enums.IntervaloUnidad;

@Data
@Entity
@Table(name = "registro_vacunas", indexes = {
        @Index(name = "idx_registro_vacunas_historia_fecha", columnList = "historia_clinica_id,fecha_aplicacion"),
        @Index(name = "idx_registro_vacunas_proxima", columnList = "fecha_proxima_dosis,activo")
})
public class RegistroVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L;

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

    @Column(name = "intervalo_cantidad")
    private Integer intervaloCantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervalo_unidad", length = 20)
    private IntervaloUnidad intervaloUnidad;

    @Column(length = 80)
    private String lote;

    @Column(name = "fecha_vencimiento_producto")
    private LocalDate fechaVencimientoProducto;

    @Column(precision = 10, scale = 3)
    private BigDecimal dosis;

    @Column(name = "unidad_dosis", length = 30)
    private String unidadDosis;

    @Column(name = "via_administracion", length = 50)
    private String viaAdministracion;

    @Column(name = "sitio_aplicacion", length = 100)
    private String sitioAplicacion;

    @Column(name = "peso_kg", precision = 8, scale = 2)
    private BigDecimal pesoKg;

    @Column(length = 500)
    private String observaciones;

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
