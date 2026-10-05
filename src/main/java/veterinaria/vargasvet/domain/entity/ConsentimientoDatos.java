package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.EstadoConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "consentimiento_datos")
public class ConsentimientoDatos {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aviso_id", nullable = false, updatable = false)
    private AvisoPrivacidad aviso;

    @Column(name = "aviso_version", nullable = false, updatable = false)
    private Integer avisoVersion;

    @Column(name = "contenido_hash", nullable = false, length = 64, updatable = false)
    private String contenidoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private FinalidadDatos finalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12, updatable = false)
    private EstadoConsentimiento estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private CanalConsentimiento canal;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", updatable = false)
    private Usuario registradoPor;

    @Column(length = 300, updatable = false)
    private String motivo;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 255, updatable = false)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
}
