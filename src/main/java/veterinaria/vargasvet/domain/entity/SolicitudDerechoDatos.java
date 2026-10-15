package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;
import veterinaria.vargasvet.domain.enums.TipoDerechoDatos;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "solicitud_derecho_datos")
public class SolicitudDerechoDatos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TipoDerechoDatos tipo;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private EstadoSolicitudDerecho estado = EstadoSolicitudDerecho.RECIBIDA;

    @Column(nullable = false, columnDefinition = "TEXT") private String descripcion;
    @Column(name = "dato_rectificar", columnDefinition = "TEXT") private String datoRectificar;
    @Column(name = "identidad_verificada", nullable = false) private boolean identidadVerificada;
    @Column(name = "metodo_verificacion", nullable = false, length = 200) private String metodoVerificacion;
    @Column(name = "creada_at", nullable = false) private LocalDateTime creadaAt;
    @Column(name = "fecha_limite", nullable = false) private LocalDate fechaLimite;
    @Column(name = "actualizada_at", nullable = false) private LocalDateTime actualizadaAt;
    @Column(columnDefinition = "TEXT") private String respuesta;
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT") private String motivoRechazo;
    @Column(name = "resuelta_at") private LocalDateTime resueltaAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resuelta_por_usuario_id") private Usuario resueltaPor;
    @Column(name = "ip_solicitud", length = 64) private String ipSolicitud;
    @Column(name = "dispositivo_solicitud", length = 255) private String dispositivoSolicitud;
}

