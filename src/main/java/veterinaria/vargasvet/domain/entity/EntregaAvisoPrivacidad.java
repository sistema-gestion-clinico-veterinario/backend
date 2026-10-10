package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import veterinaria.vargasvet.domain.enums.EstadoEntregaAviso;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "entrega_aviso_privacidad", uniqueConstraints = @UniqueConstraint(
        name = "uq_entrega_aviso_usuario_version_canal",
        columnNames = {"usuario_id", "aviso_id", "canal"}))
public class EntregaAvisoPrivacidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    @ToString.Exclude
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aviso_id", nullable = false, updatable = false)
    @ToString.Exclude
    private AvisoPrivacidad aviso;

    @Column(nullable = false, length = 20, updatable = false)
    private String canal;

    @Column(nullable = false, length = 320, updatable = false)
    private String destino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoEntregaAviso estado;

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private LocalDateTime solicitadoEn;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(length = 300)
    private String detalle;
}
