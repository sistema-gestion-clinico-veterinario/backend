package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.CanalSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "solicitud_cancelacion_datos")
public class SolicitudCancelacionDatos {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CanalSolicitudCancelacion canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoSolicitudCancelacion estado = EstadoSolicitudCancelacion.PENDIENTE;

    @Column(name = "identidad_verificada", nullable = false)
    private boolean identidadVerificada;

    @Column(name = "metodo_verificacion", nullable = false, length = 200)
    private String metodoVerificacion;

    @Column(name = "motivo_titular", length = 500)
    private String motivoTitular;

    @Column(name = "registrada_por_usuario_id")
    private Integer registradaPorUsuarioId;

    @Column(name = "creada_at", nullable = false)
    private LocalDateTime creadaAt;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(name = "resuelta_por_usuario_id")
    private Integer resueltaPorUsuarioId;

    @Column(name = "resuelta_at")
    private LocalDateTime resueltaAt;

    @Column(name = "motivo_resolucion", length = 500)
    private String motivoResolucion;

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;
}
