package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "mascota_persona_relacion", uniqueConstraints = {
        @UniqueConstraint(name = "uq_mascota_persona_relacion_uuid", columnNames = "uuid")
}, indexes = {
        @Index(name = "idx_mascota_persona_relacion_mascota", columnList = "mascota_id,activo"),
        @Index(name = "idx_mascota_persona_relacion_apoderado", columnList = "apoderado_id,activo"),
        @Index(name = "idx_mascota_persona_relacion_company", columnList = "company_id,activo")
})
public class MascotaPersonaRelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_relacion", nullable = false, length = 40)
    private TipoRelacionMascota tipoRelacion;

    @Column(name = "puede_recibir_informacion", nullable = false)
    private Boolean puedeRecibirInformacion = false;

    @Column(name = "puede_autorizar_atencion", nullable = false)
    private Boolean puedeAutorizarAtencion = false;

    @Column(name = "puede_realizar_pagos", nullable = false)
    private Boolean puedeRealizarPagos = false;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

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

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by", length = 150)
    private String revokedBy;

    @PrePersist
    protected void onCreate() {
        if (uuid == null || uuid.isBlank()) uuid = UUID.randomUUID().toString();
        if (fechaInicio == null) fechaInicio = AppClock.today();
        createdAt = AppClock.now();
        updatedAt = AppClock.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = AppClock.now();
    }
}
