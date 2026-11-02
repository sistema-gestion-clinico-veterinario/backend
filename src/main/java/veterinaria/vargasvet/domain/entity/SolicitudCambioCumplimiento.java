package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "solicitud_cambio_cumplimiento")
public class SolicitudCambioCumplimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_code", nullable = false, length = 50)
    private ControlCumplimientoClinica control;

    @Column(name = "habilitado_solicitado", nullable = false)
    private boolean habilitadoSolicitado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_solicitado", nullable = false, length = 20)
    private ModoGestionCumplimiento modoSolicitado;

    @Column(name = "bloqueante_solicitado", nullable = false)
    private boolean bloqueanteSolicitado;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "evidencia_referencia", length = 200)
    private String evidenciaReferencia;

    @Column(name = "vigente_desde_solicitada")
    private LocalDateTime vigenteDesdeSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudCambioCumplimiento estado = EstadoSolicitudCambioCumplimiento.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitada_por_usuario_id", nullable = false)
    private Usuario solicitadaPor;

    @Column(name = "solicitada_at", nullable = false)
    private LocalDateTime solicitadaAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_por_usuario_id")
    private Usuario revisionPor;

    @Column(name = "revision_at")
    private LocalDateTime revisionAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por_usuario_id")
    private Usuario resueltaPor;

    @Column(name = "resuelta_at")
    private LocalDateTime resueltaAt;

    @Column(name = "comentario_resolucion", columnDefinition = "TEXT")
    private String comentarioResolucion;

    @Column(name = "vista_clinica_at")
    private LocalDateTime vistaClinicaAt;
}
