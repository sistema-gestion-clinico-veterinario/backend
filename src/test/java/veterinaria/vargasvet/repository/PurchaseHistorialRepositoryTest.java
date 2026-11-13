package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PurchaseHistorialRepositoryTest {

    private static final Pageable PAGINA = PageRequest.of(0, 10, Sort.by("createdAt").descending());

    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private EntityManager entityManager;

    private Company clinica;
    private Company otraClinica;
    private Mascota rocky;
    private Mascota luna;
    private Mascota deOtraClinica;
    private Usuario ana;
    private Usuario beto;

    @BeforeEach
    void datos() {
        clinica = clinica("A");
        otraClinica = clinica("B");
        ana = usuario("ana", clinica);
        beto = usuario("beto", clinica);
        rocky = mascota("Rocky", apoderado(ana, clinica));
        luna = mascota("Luna", apoderado(beto, clinica));
        deOtraClinica = mascota("Toby", apoderado(usuario("carla", otraClinica), otraClinica));
    }

    @Test
    void devuelveSoloLosPagosDeLaEmpresaPedida() {
        Purchase propio = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(otraClinica, deOtraClinica, ana, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);

        Page<Purchase> historial = historial(clinica, null, null, null, null, null, null);

        assertThat(historial.getContent()).extracting(Purchase::getId).containsExactly(propio.getId());
        assertThat(historial.getTotalElements()).isEqualTo(1);
    }

    @Test
    void losOrdenaDelMasRecienteAlMasAntiguo() {
        Purchase antiguo = pago(clinica, rocky, ana, "2026-11-01T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase reciente = pago(clinica, luna, beto, "2026-11-12T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase medio = pago(clinica, rocky, ana, "2026-11-05T10:00", PaymentStatus.PAID, MetodoPago.YAPE);

        assertThat(historial(clinica, null, null, null, null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(reciente.getId(), medio.getId(), antiguo.getId());
    }

    @Test
    void soloIncluyeElTipoPedido() {
        Purchase deServicio = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase deTienda = pago(clinica, null, ana, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);
        deTienda.setTipoPurchase(TipoPurchase.TIENDA);
        entityManager.flush();

        assertThat(historial(clinica, null, null, null, null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(deServicio.getId());
    }

    @Test
    void filtraPorCliente() {
        Purchase deAna = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(clinica, luna, beto, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);

        assertThat(historial(clinica, ana.getId(), null, null, null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(deAna.getId());
    }

    @Test
    void filtraPorMascota() {
        pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase deLuna = pago(clinica, luna, beto, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);

        assertThat(historial(clinica, null, luna.getId(), null, null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(deLuna.getId());
    }

    @Test
    void filtraPorRangoDeFechasIncluyendoLosExtremos() {
        pago(clinica, rocky, ana, "2026-11-01T09:59", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase inicio = pago(clinica, rocky, ana, "2026-11-01T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase fin = pago(clinica, rocky, ana, "2026-11-30T23:59", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(clinica, rocky, ana, "2026-12-01T00:00", PaymentStatus.PAID, MetodoPago.YAPE);

        Page<Purchase> historial = historial(clinica, null, null, LocalDateTime.parse("2026-11-01T10:00"),
                LocalDateTime.parse("2026-11-30T23:59"), null, null);

        assertThat(historial.getContent()).extracting(Purchase::getId).containsExactly(fin.getId(), inicio.getId());
    }

    @Test
    void soloConFechaDesdeOSoloConFechaHasta() {
        Purchase viejo = pago(clinica, rocky, ana, "2026-10-01T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        Purchase nuevo = pago(clinica, rocky, ana, "2026-11-20T10:00", PaymentStatus.PAID, MetodoPago.YAPE);

        assertThat(historial(clinica, null, null, LocalDateTime.parse("2026-11-01T00:00"), null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(nuevo.getId());
        assertThat(historial(clinica, null, null, null, LocalDateTime.parse("2026-11-01T00:00"), null, null).getContent())
                .extracting(Purchase::getId).containsExactly(viejo.getId());
    }

    @Test
    void filtraPorEstadoYMetodoDePago() {
        Purchase pagadoConYape = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(clinica, rocky, ana, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.EFECTIVO);
        pago(clinica, rocky, ana, "2026-11-10T12:00", PaymentStatus.PENDING, MetodoPago.YAPE);

        assertThat(historial(clinica, null, null, null, null, PaymentStatus.PAID, MetodoPago.YAPE).getContent())
                .extracting(Purchase::getId).containsExactly(pagadoConYape.getId());
        assertThat(historial(clinica, null, null, null, null, PaymentStatus.PAID, null).getContent()).hasSize(2);
        assertThat(historial(clinica, null, null, null, null, null, MetodoPago.YAPE).getContent()).hasSize(2);
    }

    @Test
    void combinaTodosLosFiltros() {
        Purchase buscado = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(clinica, rocky, ana, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.EFECTIVO);
        pago(clinica, luna, beto, "2026-11-10T12:00", PaymentStatus.PAID, MetodoPago.YAPE);

        Page<Purchase> historial = historial(clinica, ana.getId(), rocky.getId(), LocalDateTime.parse("2026-11-01T00:00"),
                LocalDateTime.parse("2026-11-30T00:00"), PaymentStatus.PAID, MetodoPago.YAPE);

        assertThat(historial.getContent()).extracting(Purchase::getId).containsExactly(buscado.getId());
    }

    @Test
    void paginaYCuentaElTotalSinTraerTodo() {
        for (int dia = 1; dia <= 5; dia++) {
            pago(clinica, rocky, ana, "2026-11-0" + dia + "T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        }

        Page<Purchase> segunda = purchaseRepository.buscarHistorialPorEmpresa(clinica.getId(), TipoPurchase.SERVICIO_CITA,
                null, null, null, null, null, null, PageRequest.of(1, 2, Sort.by("createdAt").descending()));

        assertThat(segunda.getTotalElements()).isEqualTo(5);
        assertThat(segunda.getTotalPages()).isEqualTo(3);
        assertThat(segunda.getContent()).extracting(p -> p.getCreatedAt().getDayOfMonth()).containsExactly(3, 2);
    }

    @Test
    void usaLaEmpresaGuardadaEnElPagoYNoLaDeLaCita() {
        Purchase sinEmpresa = pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        sinEmpresa.setCompany(null);
        Purchase conEmpresa = pago(clinica, luna, beto, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);
        entityManager.flush();

        assertThat(historial(clinica, null, null, null, null, null, null).getContent())
                .extracting(Purchase::getId).containsExactly(conEmpresa.getId());
    }

    @Test
    void sinEmpresaNoDevuelvePagosDeNingunaClinica() {
        pago(clinica, rocky, ana, "2026-11-10T10:00", PaymentStatus.PAID, MetodoPago.YAPE);
        pago(otraClinica, deOtraClinica, ana, "2026-11-10T11:00", PaymentStatus.PAID, MetodoPago.YAPE);

        Page<Purchase> historial = purchaseRepository.buscarHistorialPorEmpresa(null, TipoPurchase.SERVICIO_CITA,
                null, null, null, null, null, null, PAGINA);

        assertThat(historial.getContent()).isEmpty();
    }

    private Page<Purchase> historial(Company empresa, Integer clienteId, Long mascotaId, LocalDateTime desde,
                                     LocalDateTime hasta, PaymentStatus estado, MetodoPago metodo) {
        entityManager.flush();
        entityManager.clear();
        return purchaseRepository.buscarHistorialPorEmpresa(empresa.getId(), TipoPurchase.SERVICIO_CITA, clienteId,
                mascotaId, desde, hasta, estado, metodo, PAGINA);
    }

    private Company clinica(String sufijo) {
        Company company = new Company();
        company.setName("Clinica " + sufijo);
        company.setSlug("clinica-" + sufijo.toLowerCase() + "-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Usuario usuario(String nombre, Company empresa) {
        Usuario usuario = new Usuario();
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@historial.test");
        usuario.setUsername(nombre + "-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(empresa);
        return usuarioRepository.save(usuario);
    }

    private Apoderado apoderado(Usuario usuario, Company empresa) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(usuario.getDni());
        apoderado.setGenero(Genero.FEMENINO);
        return apoderadoRepository.save(apoderado);
    }

    private Mascota mascota(String nombre, Apoderado apoderado) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        return mascotaRepository.save(mascota);
    }

    private Purchase pago(Company empresa, Mascota mascota, Usuario cliente, String creadoEn, PaymentStatus estado,
                          MetodoPago metodo) {
        Cita cita = null;
        if (mascota != null) {
            LocalDateTime inicio = LocalDateTime.parse(creadoEn);
            cita = new Cita();
            cita.setMascota(mascota);
            cita.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 12));
            cita.setMotivoCita("Control");
            cita.setFechaHoraInicio(inicio);
            cita.setFechaHoraFin(inicio.plusMinutes(30));
            cita.setDuracionMinutos(30);
            cita.setEstado(EstadoCita.COMPLETADA);
            cita.setTotalServicio(new BigDecimal("100.00"));
            cita.setMontoPagado(new BigDecimal("100.00"));
            cita.setEliminada(false);
            cita.setEsEmergencia(false);
            cita = citaRepository.save(cita);
        }
        Purchase purchase = new Purchase();
        purchase.setCompany(empresa);
        purchase.setCita(cita);
        purchase.setCliente(cliente);
        purchase.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        purchase.setPaymentStatus(estado);
        purchase.setMetodoPago(metodo);
        purchase.setTotal(new BigDecimal("100.00"));
        purchase.setCreatedAt(LocalDateTime.parse(creadoEn));
        return purchaseRepository.save(purchase);
    }
}
