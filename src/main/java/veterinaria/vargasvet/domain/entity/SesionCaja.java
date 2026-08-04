package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sesion_caja")
public class SesionCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoSesionCaja estado;

    @Column(name = "monto_apertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "abierta_at", nullable = false)
    private LocalDateTime abiertaAt;

    @Column(name = "cerrada_at")
    private LocalDateTime cerradaAt;

    @Column(name = "abierta_por", nullable = false, length = 150)
    private String abiertaPor;

    @Column(name = "cerrada_por", length = 150)
    private String cerradaPor;

    @Column(length = 300)
    private String observaciones;
}
