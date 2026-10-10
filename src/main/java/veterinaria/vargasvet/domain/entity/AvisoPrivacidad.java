package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "aviso_privacidad", uniqueConstraints = {
        @UniqueConstraint(name = "uq_aviso_privacidad_company_audiencia_version",
                columnNames = {"company_id", "audiencia", "version"})
})
public class AvisoPrivacidad {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AudienciaAvisoPrivacidad audiencia;

    @Column(nullable = false, updatable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String contenido;

    @Column(name = "contenido_hash", nullable = false, length = 64, updatable = false)
    private String contenidoHash;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String campos;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private LocalDateTime vigenteDesde;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "creado_por", length = 150, updatable = false)
    private String creadoPor;

    @Column(name = "creado_ip", length = 64, updatable = false)
    private String creadoIp;

    @Column(name = "creado_dispositivo", length = 120, updatable = false)
    private String creadoDispositivo;
}
