package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.ServiciosVeterinarios;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoPurchase;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest
class PagoServiceIntegrationTest {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private ServiciosVeterinariosRepository serviciosVeterinariosRepository;

    private AuditLogService auditLogService;
    private CajaService cajaService;
    private PagoServiceImpl pagoService;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        cajaService = mock(CajaService.class);
        pagoService = new PagoServiceImpl(
                citaRepository,
                purchaseRepository,
                usuarioRepository,
                auditLogService,
                cajaService
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarPagoEfectivoCompletoPersistePurchaseYActualizaCita() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, BigDecimal.ZERO);

        PagoResponse response = pagoService.registrar(pagoEfectivo(cita.getId(), new BigDecimal("120.00")));

        Optional<Purchase> persisted = purchaseRepository
                .findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(cita.getId(), TipoPurchase.SERVICIO_CITA);
        Cita citaActualizada = citaRepository.findById(cita.getId()).orElseThrow();

        assertThat(response.getEstado()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getCambio()).isEqualByComparingTo("20.00");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(persisted.get().getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(citaActualizada.getMontoPagado()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Yape se registra como método manual sin pasarela externa")
    void registrarPagoYapeManualPersistePago() {
        Cita cita = crearCita(EstadoCita.PROGRAMADA, BigDecimal.ZERO);

        PagoResponse response = pagoService.registrar(pagoYape(cita.getId()));

        Purchase persisted = purchaseRepository
                .findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(cita.getId(), TipoPurchase.SERVICIO_CITA)
                .orElseThrow();
        Cita citaActualizada = citaRepository.findById(cita.getId()).orElseThrow();

        assertThat(response.getEstado()).isEqualTo(PaymentStatus.PAID);
        assertThat(persisted.getMetodoPago()).isEqualTo(MetodoPago.YAPE);
        assertThat(persisted.getMercadoPagoId()).isNull();
        assertThat(citaActualizada.getMontoPagado()).isEqualByComparingTo("100.00");
    }

    @Test
    void registrarPagoRechazaCitaCanceladaSinPersistirPurchase() {
        Cita cita = crearCita(EstadoCita.CANCELADA, BigDecimal.ZERO);

        assertThatThrownBy(() -> pagoService.registrar(pagoEfectivo(cita.getId(), new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se puede registrar un pago para una cita con estado: CANCELADA");

        assertThat(purchaseRepository.findTopByCitaIdAndTipoPurchaseOrderByCreatedAtDesc(
                cita.getId(),
                TipoPurchase.SERVICIO_CITA
        )).isEmpty();
    }

    private PagoRequest pagoEfectivo(Long citaId, BigDecimal montoRecibido) {
        PagoRequest request = new PagoRequest();
        request.setCitaId(citaId);
        request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setMontoRecibido(montoRecibido);
        return request;
    }

    private PagoRequest pagoYape(Long citaId) {
        PagoRequest request = new PagoRequest();
        request.setCitaId(citaId);
        request.setMetodoPago(MetodoPago.YAPE);
        return request;
    }

    private Cita crearCita(EstadoCita estado, BigDecimal montoPagado) {
        Company company = new Company();
        company.setName("VargasVet Test");
        company.setRuc(uniqueDigits(11));
        company.setActivo(true);
        company = companyRepository.save(company);

        Usuario apoderadoUser = usuario("cliente", company);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(apoderadoUser);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(uniqueDigits(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Usuario empleadoUser = usuario("vet", company);
        Empleado empleado = new Empleado();
        empleado.setUser(empleadoUser);
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setNumeroDocumentoIdentidad(uniqueDigits(8));
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado = empleadoRepository.save(empleado);

        ServiciosVeterinarios servicio = new ServiciosVeterinarios();
        servicio.setCompany(company);
        servicio.setNombre("Consulta general");
        servicio.setDescripcion("Consulta veterinaria general");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDisponible(true);
        servicio.setActivo(true);
        servicio.setDuracionEstimada(30);
        servicio.setPermiteEmergencia(false);
        servicio = serviciosVeterinariosRepository.save(servicio);

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(empleado);
        cita.setServicio(servicio);
        cita.setMotivoCita("Control general");
        cita.setFechaHoraInicio(LocalDateTime.now().plusDays(1));
        cita.setFechaHoraFin(LocalDateTime.now().plusDays(1).plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(estado);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(montoPagado);
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        Cita saved = citaRepository.save(cita);
        UsuarioPrincipal principal = new UsuarioPrincipal(
                apoderadoUser.getId(), apoderadoUser.getEmail(), apoderadoUser.getPassword(), List.of(), company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return saved;
    }

    private Usuario usuario(String prefix, Company company) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefix + "-" + UUID.randomUUID() + "@vargasvet.test");
        usuario.setPassword("password");
        usuario.setNombre(prefix);
        usuario.setApellido("Test");
        usuario.setDni(uniqueDigits(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(company);
        return usuarioRepository.save(usuario);
    }

    private String uniqueDigits(int length) {
        String digits = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digits.length() < length) {
            digits += "0";
        }
        return digits.substring(0, length);
    }
}
