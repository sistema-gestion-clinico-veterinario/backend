package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "cierre_cuenta")
public class CierreCuenta {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoCierreCuenta estado;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "empleado_id")
    private Long empleadoId;

    @Column(name = "apoderado_id")
    private Long apoderadoId;

    @Column(name = "cerrada_at", nullable = false)
    private LocalDateTime cerradaAt;

    @Column(name = "vence_at", nullable = false)
    private LocalDateTime venceAt;

    @Column(name = "reactivada_at")
    private LocalDateTime reactivadaAt;

    @Column(name = "purgada_at")
    private LocalDateTime purgadaAt;
}
