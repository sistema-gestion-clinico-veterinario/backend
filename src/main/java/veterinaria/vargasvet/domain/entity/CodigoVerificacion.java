package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "codigo_verificacion")
public class CodigoVerificacion {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 30)
    private String proposito;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(nullable = false, length = 32)
    private String sal;

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "creado_at", nullable = false)
    private LocalDateTime creadoAt;

    @Column(name = "expira_at", nullable = false)
    private LocalDateTime expiraAt;

    @Column(name = "usado_at")
    private LocalDateTime usadoAt;
}
