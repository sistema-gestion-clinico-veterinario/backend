package veterinaria.vargasvet.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.entity.Raza;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.domain.enums.TipoPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DashboardConsultasOptimizadasTest {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Company clinica;
    private Company otraClinica;
    private Usuario cliente;
    private Apoderado propietario;
    private Apoderado propietarioAjeno;

    @BeforeEach
    void datos() {
        clinica = clinica("Uno");
        otraClinica = clinica("Dos");
        cliente = usuario("ana", clinica);
        propietario = apoderado(cliente, clinica);
        propietarioAjeno = apoderado(usuario("carla", otraClinica), otraClinica);
    }

    @Test
    void laListaDeMascotasDelPanelEsIgualALaDelCodigoAnteriorYNoCuentaElTotal() {
        Raza labrador = raza("Labrador");
        mascota("Zeus", propietario, true, labrador);
        mascota("Ana", propietario, true, null);
        mascota("Bruno", propietario, false, null);
        mascota("Coco", propietario, true, labrador);
        mascota("Dana", propietario, true, null);
        mascota("Eli", propietario, true, null);
        mascota("Fido", propietario, true, null);
        mascota("Gala", propietario, true, null);
        mascota("Aaa", propietarioAjeno, true, null);
        Statistics estadisticas = estadisticas();

        Page<Mascota> anterior = mascotaRepository.buscar(clinica.getId(), null, null, null, null, null, PageRequest.of(0, 6));
        long sentenciasAnteriores = estadisticas.getPrepareStatementCount();
        List<String> esperado = anterior.getContent().stream().map(this::resumen).toList();

        estadisticas = estadisticas();
        List<MascotaRepository.MascotaDelPanel> nuevo = mascotaRepository.primerasPorNombre(clinica.getId(), PageRequest.of(0, 6));
        long sentenciasNuevas = estadisticas.getPrepareStatementCount();

        assertThat(nuevo.stream().map(this::resumen).toList()).containsExactlyElementsOf(esperado);
        assertThat(esperado).hasSize(6).first().asString().contains("|Ana|");
        assertThat(esperado).anyMatch(texto -> texto.contains("Bruno|"));
        assertThat(sentenciasAnteriores).isEqualTo(2);
        assertThat(sentenciasNuevas).isEqualTo(1);
    }

    @Test
    void sinMascotasLaListaDelPanelEstaVacia() {
        assertThat(mascotaRepository.primerasPorNombre(clinica.getId(), PageRequest.of(0, 6))).isEmpty();
        assertThat(mascotaRepository.primerasPorNombre(otraClinica.getId() + 999, PageRequest.of(0, 6))).isEmpty();
    }

    @Test
    void losUltimosPagosDelPanelSonLosMismosQueLosDelCodigoAnteriorSinContarElTotal() {
        Mascota rocky = mascota("Rocky", propietario, true, null);
        Mascota toby = mascota("Toby", propietarioAjeno, true, null);
        pago(clinica, rocky, cliente, "2026-11-01T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-02T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-03T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-04T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-05T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-06T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, rocky, cliente, "2026-11-07T10:00", TipoPurchase.SERVICIO_CITA);
        pago(clinica, null, cliente, "2026-11-20T10:00", TipoPurchase.TIENDA);
        pago(otraClinica, toby, cliente, "2026-11-21T10:00", TipoPurchase.SERVICIO_CITA);
        Statistics estadisticas = estadisticas();

        Page<Long> anterior = purchaseRepository.buscarIdsDelHistorial(clinica.getId(), TipoPurchase.SERVICIO_CITA,
                null, null, null, null, null, null, PageRequest.of(0, 5, Sort.by("createdAt").descending()));
        long sentenciasAnteriores = estadisticas.getPrepareStatementCount();

        estadisticas = estadisticas();
        List<Long> nuevo = purchaseRepository.ultimosIdsDelHistorial(clinica.getId(), TipoPurchase.SERVICIO_CITA, PageRequest.of(0, 5));
        long sentenciasNuevas = estadisticas.getPrepareStatementCount();

        assertThat(nuevo).containsExactlyElementsOf(anterior.getContent()).hasSize(5);
        assertThat(sentenciasAnteriores).isEqualTo(2);
        assertThat(sentenciasNuevas).isEqualTo(1);
    }

    @Test
    void lasCitasTotalesYDeHoyCoincidenConLasDosConsultasAnterioresEnUnaSola() {
        Mascota luna = mascota("Luna", propietario, true, null);
        Mascota ajena = mascota("Ajena", propietarioAjeno, true, null);
        LocalDate hoy = LocalDate.now();
        cita(luna, hoy.atTime(LocalTime.MIN), false);
        cita(luna, hoy.atTime(12, 0), false);
        cita(luna, hoy.atTime(23, 59, 59), false);
        cita(luna, hoy.plusDays(1).atTime(9, 0), false);
        cita(luna, hoy.minusDays(1).atTime(9, 0), false);
        cita(luna, hoy.atTime(15, 0), true);
        cita(ajena, hoy.atTime(10, 0), false);
        Statistics estadisticas = estadisticas();

        long total = citaRepository.countByCompanyId(clinica.getId());
        long deHoy = citaRepository.countTodayByCompanyId(clinica.getId());
        long sentenciasAnteriores = estadisticas.getPrepareStatementCount();

        estadisticas = estadisticas();
        CitaRepository.TotalesCitas totales = citaRepository.totalesPorCompany(clinica.getId());
        long sentenciasNuevas = estadisticas.getPrepareStatementCount();

        assertThat(totales.getTotal()).isEqualTo(total).isEqualTo(5L);
        assertThat(totales.getHoy()).isEqualTo(deHoy).isEqualTo(3L);
        assertThat(sentenciasAnteriores).isEqualTo(2);
        assertThat(sentenciasNuevas).isEqualTo(1);
    }

    @Test
    void sinCitasLosTotalesSonCeroYNoNulos() {
        CitaRepository.TotalesCitas totales = citaRepository.totalesPorCompany(clinica.getId());

        assertThat(totales.getTotal()).isZero();
        assertThat(totales.getHoy()).isZero();
    }

    private Statistics estadisticas() {
        entityManager.flush();
        entityManager.clear();
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();
        return estadisticas;
    }

    private String resumen(Mascota m) {
        return m.getId() + "|" + m.getNombreCompleto() + "|" + m.getEspecie() + "|"
                + (m.getRaza() != null ? m.getRaza().getNombre() : null);
    }

    private String resumen(MascotaRepository.MascotaDelPanel m) {
        return m.getId() + "|" + m.getNombreCompleto() + "|" + m.getEspecie() + "|" + m.getRaza();
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
        usuario.setEmail(nombre + "-" + UUID.randomUUID() + "@panel.test");
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

    private Raza raza(String nombre) {
        Raza raza = new Raza();
        raza.setNombre(nombre);
        raza.setEspecie(EspecieMascota.PERRO);
        entityManager.persist(raza);
        return raza;
    }

    private Mascota mascota(String nombre, Apoderado dueno, boolean activa, Raza raza) {
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto(nombre);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(dueno);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota.setActivo(activa);
        mascota.setRaza(raza);
        return mascotaRepository.save(mascota);
    }

    private Cita cita(Mascota mascota, LocalDateTime inicio, boolean eliminada) {
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 12));
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(inicio);
        cita.setFechaHoraFin(inicio.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.COMPLETADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(new BigDecimal("100.00"));
        cita.setEliminada(eliminada);
        cita.setEsEmergencia(false);
        return citaRepository.save(cita);
    }

    private Purchase pago(Company empresa, Mascota mascota, Usuario cliente, String creadoEn, TipoPurchase tipo) {
        Cita cita = mascota != null ? cita(mascota, LocalDateTime.parse(creadoEn), false) : null;
        Purchase purchase = new Purchase();
        purchase.setCompany(empresa);
        purchase.setCita(cita);
        purchase.setCliente(cliente);
        purchase.setTipoPurchase(tipo);
        purchase.setPaymentStatus(PaymentStatus.PAID);
        purchase.setMetodoPago(MetodoPago.YAPE);
        purchase.setTotal(new BigDecimal("100.00"));
        purchase.setCreatedAt(LocalDateTime.parse(creadoEn));
        return purchaseRepository.save(purchase);
    }
}
