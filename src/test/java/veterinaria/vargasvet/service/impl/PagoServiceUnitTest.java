package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceUnitTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CajaService cajaService;

    @Test
    void registrar_rechazaCitaCancelada() {
        PagoServiceImpl service = service();
        PagoRequest request = efectivoRequest(10L, new BigDecimal("100.00"));
        Cita cita = cita(10L, EstadoCita.CANCELADA, BigDecimal.ZERO);
        when(citaRepository.findByIdAndCompanyIdForUpdate(10L, 3)).thenReturn(Optional.of(cita));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(request)
        );

        assertEquals("No se puede registrar un pago para una cita con estado: CANCELADA", ex.getMessage());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void registrar_completaSaldoDeCitaConPagoParcialPrevio() {
        PagoServiceImpl service = service();
        PagoRequest request = efectivoRequest(10L, new BigDecimal("100.00"));
        Cita cita = cita(10L, EstadoCita.PROGRAMADA, new BigDecimal("80.00"));
        when(citaRepository.findByIdAndCompanyIdForUpdate(10L, 3)).thenReturn(Optional.of(cita));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase pago = invocation.getArgument(0);
            pago.setId(87L);
            return pago;
        });

        PagoResponse response = service.registrar(request);

        assertEquals(new BigDecimal("20.00"), response.getMonto());
        assertEquals(new BigDecimal("80.00"), response.getCambio());
        assertEquals(new BigDecimal("0.00"), response.getSaldoPendiente());
        assertEquals(new BigDecimal("100.00"), cita.getMontoPagado());
    }

    @Test
    void registrar_rechazaPagoEfectivoSinMontoRecibido() {
        PagoServiceImpl service = service();
        PagoRequest request = efectivoRequest(10L, null);
        Cita cita = cita(10L, EstadoCita.PROGRAMADA, BigDecimal.ZERO);
        when(citaRepository.findByIdAndCompanyIdForUpdate(10L, 3)).thenReturn(Optional.of(cita));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.registrar(request)
        );

        assertEquals("El monto recibido es obligatorio para pagos en efectivo", ex.getMessage());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void registrar_pagoEfectivoCompletoGuardaPagoPaidYCalculaCambio() {
        PagoServiceImpl service = service();
        Cita cita = cita(10L, EstadoCita.PROGRAMADA, BigDecimal.ZERO);
        PagoRequest request = efectivoRequest(10L, new BigDecimal("120.00"));
        when(citaRepository.findByIdAndCompanyIdForUpdate(10L, 3)).thenReturn(Optional.of(cita));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase pago = invocation.getArgument(0);
            pago.setId(88L);
            pago.setCreatedAt(LocalDateTime.of(2026, 7, 7, 10, 0));
            return pago;
        });

        PagoResponse response = service.registrar(request);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(captor.capture());
        Purchase saved = captor.getValue();
        assertEquals(PaymentStatus.PAID, saved.getPaymentStatus());
        assertEquals(TipoPurchase.SERVICIO_CITA, saved.getTipoPurchase());
        assertEquals(new BigDecimal("20.00"), response.getCambio());
        assertEquals(new BigDecimal("100.00"), cita.getMontoPagado());
        verify(auditLogService).log(eq("REGISTRAR_PAGO"), eq("Facturación"), any(String.class));
    }

    @Test
    void obtenerPorCita_calculaCambioCuandoMontoRecibidoSuperaTotal() {
        PagoServiceImpl service = service();
        Cita cita = cita(10L, EstadoCita.PROGRAMADA, BigDecimal.ZERO);
        Purchase pago = new Purchase();
        pago.setId(90L);
        pago.setCita(cita);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setTotal(new BigDecimal("100.00"));
        pago.setMontoRecibido(new BigDecimal("130.00"));
        pago.setPaymentStatus(PaymentStatus.PAID);
        when(citaRepository.findByIdAndCompanyId(10L, 3)).thenReturn(Optional.of(cita));
        when(purchaseRepository.findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(10L, TipoPurchase.SERVICIO_CITA))
                .thenReturn(Optional.of(pago));

        PagoResponse response = service.obtenerPorCita(10L);

        assertEquals(new BigDecimal("30.00"), response.getCambio());
        assertEquals(PaymentStatus.PAID, response.getEstado());
    }

    @Test
    void registrar_noExponeNiModificaCitaDeOtraEmpresa() {
        PagoServiceImpl service = service();
        authenticateTenant(3);
        PagoRequest request = efectivoRequest(44L, new BigDecimal("100.00"));
        when(citaRepository.findByIdAndCompanyIdForUpdate(44L, 3)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(request));

        verify(citaRepository, never()).findByIdForUpdate(44L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
        verify(cajaService, never()).registrarIngresoPorCita(any(), any(), any(), any());
    }

    private PagoServiceImpl service() {
        return new PagoServiceImpl(
                citaRepository,
                purchaseRepository,
                usuarioRepository,
                auditLogService,
                cajaService
        );
    }

    private PagoRequest efectivoRequest(Long citaId, BigDecimal montoRecibido) {
        PagoRequest request = new PagoRequest();
        request.setCitaId(citaId);
        request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setMontoRecibido(montoRecibido);
        return request;
    }

    private Cita cita(Long id, EstadoCita estado, BigDecimal montoPagado) {
        Company company = new Company();
        company.setId(3);

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Lopez");
        usuario.setCompany(company);

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setApoderado(apoderado);

        Cita cita = new Cita();
        cita.setId(id);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(montoPagado);
        cita.setMascota(mascota);
        authenticateTenant(company.getId());
        return cita;
    }

    private void authenticateTenant(Integer companyId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(99, "empleado@test.local", "", List.of(), companyId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
