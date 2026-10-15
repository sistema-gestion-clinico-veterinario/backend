package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import veterinaria.vargasvet.domain.enums.EstadoConsentimientoVeterinario;

import java.time.LocalDateTime;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "consentimiento_informado_veterinario")
public class ConsentimientoInformadoVeterinario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include private Long id;
    @Column(nullable = false, unique = true, length = 36, updatable = false) private String uuid;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "mascota_id") private Mascota mascota;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "consulta_id") private Consulta consulta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "apoderado_id") private Apoderado apoderado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "creado_por_usuario_id") private Usuario creadoPor;
    @Column(nullable = false, length = 200) private String procedimiento;
    @Column(name = "diagnostico_indicacion", nullable = false, columnDefinition = "TEXT") private String diagnosticoIndicacion;
    @Column(name = "descripcion_procedimiento", nullable = false, columnDefinition = "TEXT") private String descripcionProcedimiento;
    @Column(name = "beneficios_esperados", nullable = false, columnDefinition = "TEXT") private String beneficiosEsperados;
    @Column(name = "riesgos_previsibles", nullable = false, columnDefinition = "TEXT") private String riesgosPrevisibles;
    @Column(nullable = false, columnDefinition = "TEXT") private String alternativas;
    @Column(name = "consecuencias_no_realizar", nullable = false, columnDefinition = "TEXT") private String consecuenciasNoRealizar;
    @Column(name = "autorizacion_anestesia", nullable = false) private boolean autorizacionAnestesia;
    @Column(name = "autorizacion_emergencia", nullable = false) private boolean autorizacionEmergencia;
    @Column(name = "preguntas_aclaradas", nullable = false) private boolean preguntasAclaradas;
    @Column(nullable = false) private int version;
    @Column(nullable = false, columnDefinition = "TEXT", updatable = false) private String contenido;
    @Column(name = "contenido_hash", nullable = false, length = 64, updatable = false) private String contenidoHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private EstadoConsentimientoVeterinario estado;
    @Column(name = "creado_at", nullable = false, updatable = false) private LocalDateTime creadoAt;
    @Column(name = "decidido_at") private LocalDateTime decididoAt;
    @Column(name = "nombre_firmante", length = 200) private String nombreFirmante;
    @Column(name = "documento_firmante", length = 40) private String documentoFirmante;
    @Column(name = "relacion_firmante", length = 80) private String relacionFirmante;
    @Column(name = "declaracion_aceptacion") private Boolean declaracionAceptacion;
    @Column(name = "motivo_rechazo", columnDefinition = "TEXT") private String motivoRechazo;
    @Column(name = "ip_decision", length = 64) private String ipDecision;
    @Column(name = "dispositivo_decision", length = 255) private String dispositivoDecision;
    @Column(name = "revocado_at") private LocalDateTime revocadoAt;
    @Column(name = "motivo_revocacion", columnDefinition = "TEXT") private String motivoRevocacion;
}
