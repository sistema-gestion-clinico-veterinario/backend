package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByCompanyIdAndIdempotencyKey(Integer companyId, String idempotencyKey);

    Optional<Purchase> findByIdAndCompanyId(Long id, Integer companyId);

    Optional<Purchase> findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(Long citaId, TipoPurchase tipoPurchase);

    List<Purchase> findByCitaIdAndTipoPurchaseAndPaymentStatusNot(
            Long citaId, TipoPurchase tipoPurchase, PaymentStatus paymentStatus);

    boolean existsByCitaIdAndTipoPurchase(Long citaId, TipoPurchase tipoPurchase);

    boolean existsByCitaIdAndTipoPurchaseAndPaymentStatusNot(Long citaId, TipoPurchase tipoPurchase, PaymentStatus paymentStatus);

    @Query("SELECT p FROM Purchase p WHERE p.cita.mascota.apoderado.user.id = :userId "
            + "AND p.cita.mascota.apoderado.company.id = :companyId AND p.tipoPurchase = :tipo ORDER BY p.createdAt DESC")
    Page<Purchase> findByApoderadoUserIdAndCompanyId(@Param("userId") Integer userId, @Param("companyId") Integer companyId,
                                                     @Param("tipo") TipoPurchase tipo, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.cita.mascota.apoderado.company.id = :companyId AND p.tipoPurchase = :tipo ORDER BY p.createdAt DESC")
    Page<Purchase> findByCompanyId(@Param("companyId") Integer companyId, @Param("tipo") TipoPurchase tipo, Pageable pageable);

    String HISTORIAL_DESDE = "FROM Purchase p LEFT JOIN p.cita c LEFT JOIN c.mascota m ";

    String HISTORIAL_FILTROS = "WHERE p.company.id = :companyId "
            + "AND p.tipoPurchase = :tipo "
            + "AND (:clienteId IS NULL OR p.cliente.id = :clienteId) "
            + "AND (:mascotaId IS NULL OR m.id = :mascotaId) "
            + "AND (CAST(:fechaDesde AS timestamp) IS NULL OR p.createdAt >= :fechaDesde) "
            + "AND (CAST(:fechaHasta AS timestamp) IS NULL OR p.createdAt <= :fechaHasta) "
            + "AND (:estado IS NULL OR p.paymentStatus = :estado) "
            + "AND (:metodoPago IS NULL OR p.metodoPago = :metodoPago) ";

    @Query(value = "SELECT p.id " + HISTORIAL_DESDE + HISTORIAL_FILTROS + "ORDER BY p.createdAt DESC",
           countQuery = "SELECT COUNT(p) " + HISTORIAL_DESDE + HISTORIAL_FILTROS)
    Page<Long> buscarIdsDelHistorial(
            @Param("companyId") Integer companyId,
            @Param("tipo") TipoPurchase tipo,
            @Param("clienteId") Integer clienteId,
            @Param("mascotaId") Long mascotaId,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("estado") PaymentStatus estado,
            @Param("metodoPago") MetodoPago metodoPago,
            Pageable pageable);

    interface PagoDelHistorial {
        Long getId();
        MetodoPago getMetodoPago();
        BigDecimal getTotal();
        BigDecimal getMontoRecibido();
        LocalDateTime getCreatedAt();
        PaymentStatus getPaymentStatus();
        Long getCitaId();
        String getNumeroCita();
        EstadoCita getEstadoCita();
        String getMascotaNombre();
        Integer getClienteUsuarioId();
        String getClienteNombre();
        String getClienteApellido();
        String getServicioNombre();
        Integer getVeterinarioUsuarioId();
        String getVeterinarioNombre();
        String getVeterinarioApellido();
        Integer getUsuarioId();
        String getUsuarioNombre();
        String getUsuarioApellido();
    }

    @Query("SELECT p.id AS id, p.metodoPago AS metodoPago, p.total AS total, p.montoRecibido AS montoRecibido, "
            + "p.createdAt AS createdAt, p.paymentStatus AS paymentStatus, "
            + "c.id AS citaId, c.numeroCita AS numeroCita, c.estado AS estadoCita, m.nombreCompleto AS mascotaNombre, "
            + "cu.id AS clienteUsuarioId, cu.nombre AS clienteNombre, cu.apellido AS clienteApellido, "
            + "s.nombre AS servicioNombre, "
            + "vu.id AS veterinarioUsuarioId, vu.nombre AS veterinarioNombre, vu.apellido AS veterinarioApellido, "
            + "pu.id AS usuarioId, pu.nombre AS usuarioNombre, pu.apellido AS usuarioApellido "
            + "FROM Purchase p "
            + "LEFT JOIN p.cita c "
            + "LEFT JOIN c.mascota m "
            + "LEFT JOIN m.apoderado a "
            + "LEFT JOIN a.user cu "
            + "LEFT JOIN c.servicio s "
            + "LEFT JOIN c.empleado e "
            + "LEFT JOIN e.user vu "
            + "LEFT JOIN p.user pu "
            + "WHERE p.id IN :ids")
    List<PagoDelHistorial> listadoDelHistorial(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Purchase p WHERE p.user.company.id = :companyId ORDER BY p.createdAt DESC")
    Page<Purchase> findByUserCompanyId(@Param("companyId") Integer companyId, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.user.company.id = :companyId AND p.cita IS NULL ORDER BY p.createdAt DESC")
    Page<Purchase> findByUserCompanyIdNonCita(@Param("companyId") Integer companyId, Pageable pageable);

    @Query("SELECT p FROM Purchase p JOIN FETCH p.cita c LEFT JOIN FETCH c.servicio " +
           "WHERE p.cita.id IN :citaIds AND p.tipoPurchase = veterinaria.vargasvet.domain.enums.TipoPurchase.SERVICIO_CITA " +
           "AND p.paymentStatus = veterinaria.vargasvet.domain.enums.PaymentStatus.PAID")
    List<Purchase> findPagadosByCitaIds(@Param("citaIds") List<Long> citaIds);

    String VENTAS_DE_TIENDA_EN_EL_PERIODO = "p.company.id = :companyId " +
           "AND p.tipoPurchase = :tipo AND p.paymentStatus = :estado " +
           "AND (:registradoPorUsuarioId IS NULL OR p.user.id = :registradoPorUsuarioId) " +
           "AND (CAST(:metodoPago AS string) IS NULL OR p.metodoPago = :metodoPago) " +
           "AND p.createdAt >= :desde AND p.createdAt < :hasta ";

    interface VentaAgrupada {
        MetodoPago getMetodoPago();
        LocalDate getDia();
        Long getVentas();
        BigDecimal getMonto();
    }

    @Query("SELECT p.metodoPago AS metodoPago, CAST(p.createdAt AS date) AS dia, COUNT(p) AS ventas, " +
           "COALESCE(SUM(p.total), 0) AS monto " +
           "FROM Purchase p WHERE " + VENTAS_DE_TIENDA_EN_EL_PERIODO +
           "GROUP BY p.metodoPago, CAST(p.createdAt AS date)")
    List<VentaAgrupada> resumirVentas(@Param("companyId") Integer companyId,
                                      @Param("tipo") TipoPurchase tipo,
                                      @Param("estado") PaymentStatus estado,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      @Param("registradoPorUsuarioId") Integer registradoPorUsuarioId,
                                      @Param("metodoPago") MetodoPago metodoPago);
}
