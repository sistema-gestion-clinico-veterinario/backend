package veterinaria.vargasvet.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
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
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.ServiciosVeterinariosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.CajaService;
import veterinaria.vargasvet.service.PetOwnershipService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PagoHistorialSentenciasTest {

    @Autowired private CitaRepository citaRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServiciosVeterinariosRepository serviciosRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private PagoServiceImpl servicio;
    private Company clinica;

    @BeforeEach
    void datos() {
        servicio = new PagoServiceImpl(citaRepository, purchaseRepository, usuarioRepository,
                mock(AuditLogService.class), mock(CajaService.class), mock(PetOwnershipService.class));
        clinica = new Company();
        clinica.setName("Clinica Pagos");
        clinica.setSlug("clinica-pagos-" + UUID.randomUUID());
        clinica.setRuc(digitos(11));
        clinica.setActivo(true);
        clinica = companyRepository.save(clinica);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(1, "caja@test", "x", List.of(), clinica.getId()), null, List.of()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unaPaginaDeVeintePagosNoLanzaUnaConsultaPorPago() {
        for (int i = 1; i <= 20; i++) {
            pago(i);
        }
        entityManager.flush();
        entityManager.clear();
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        Page<PagoListResponse> pagina = servicio.listarHistorialPorEmpresa(0, 20, clinica.getId(), null, null, null, null, null, null);

        assertThat(pagina.getContent()).hasSize(20);
        assertThat(estadisticas.getPrepareStatementCount()).as("sentencias por página de 20 pagos").isEqualTo(3);
    }

    @Test
    void cadaFilaTraeLosNombresDeLaMascotaElClienteElServicioYElVeterinario() {
        pago(1);
        entityManager.flush();
        entityManager.clear();

        PagoListResponse fila = servicio.listarHistorialPorEmpresa(0, 20, clinica.getId(), null, null, null, null, null, null)
                .getContent().get(0);

        assertThat(fila.getMascotaNombre()).isEqualTo("Mascota 1");
        assertThat(fila.getClienteNombre()).isEqualTo("Cliente 1 Apellido");
        assertThat(fila.getServicioNombre()).isEqualTo("Servicio 1");
        assertThat(fila.getVeterinarioNombre()).isEqualTo("Vet 1 Apellido");
        assertThat(fila.getNumeroCita()).startsWith("CIT-");
        assertThat(fila.getMonto()).isEqualByComparingTo("100.00");
        assertThat(fila.getEstado()).isEqualTo(PaymentStatus.PAID);
        assertThat(fila.getMetodoPago()).isEqualTo(MetodoPago.YAPE);
    }

    @Test
    void losOrdenaDelMasRecienteAlMasAntiguo() {
        pago(1);
        pago(3);
        pago(2);
        entityManager.flush();
        entityManager.clear();

        List<PagoListResponse> filas = servicio.listarHistorialPorEmpresa(0, 20, clinica.getId(), null, null, null, null, null, null)
                .getContent();

        assertThat(filas).extracting(PagoListResponse::getMascotaNombre).containsExactly("Mascota 3", "Mascota 2", "Mascota 1");
    }

    @Test
    void unaCitaSinNumeroRecibeUnoAlListarComoAntes() {
        Purchase pago = pago(1);
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE citas SET numero_cita = NULL WHERE id = :id")
                .setParameter("id", pago.getCita().getId()).executeUpdate();
        entityManager.clear();

        PagoListResponse fila = servicio.listarHistorialPorEmpresa(0, 20, clinica.getId(), null, null, null, null, null, null)
                .getContent().get(0);
        entityManager.flush();
        entityManager.clear();

        assertThat(fila.getNumeroCita()).startsWith("CIT-2026");
        assertThat(citaRepository.findById(pago.getCita().getId()).orElseThrow().getNumeroCita()).isEqualTo(fila.getNumeroCita());
    }

    private Purchase pago(int n) {
        Usuario cliente = usuario("Cliente " + n);
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(cliente);
        apoderado.setCompany(clinica);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(digitos(8));
        apoderado.setGenero(Genero.FEMENINO);
        apoderado = apoderadoRepository.save(apoderado);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Mascota " + n);
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);
        mascota.setUuid(UUID.randomUUID().toString());
        mascota = mascotaRepository.save(mascota);

        Empleado vet = new Empleado();
        vet.setUser(usuario("Vet " + n));
        vet.setCompany(clinica);
        vet.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        vet.setNumeroDocumentoIdentidad(digitos(8));
        vet.setGenero(Genero.MASCULINO);
        vet.setEstado(true);
        vet = empleadoRepository.save(vet);

        ServiciosVeterinarios servicioVet = new ServiciosVeterinarios();
        servicioVet.setCompany(clinica);
        servicioVet.setNombre("Servicio " + n);
        servicioVet.setDescripcion("Servicio de prueba");
        servicioVet.setPrecio(new BigDecimal("100.00"));
        servicioVet.setDisponible(true);
        servicioVet.setActivo(true);
        servicioVet.setDuracionEstimada(30);
        servicioVet.setPermiteEmergencia(false);
        servicioVet = serviciosRepository.save(servicioVet);

        LocalDateTime fecha = LocalDateTime.parse("2026-11-01T10:00").plusDays(n);
        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setEmpleado(vet);
        cita.setServicio(servicioVet);
        cita.setNumeroCita("CIT-" + UUID.randomUUID().toString().substring(0, 12));
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(fecha);
        cita.setFechaHoraFin(fecha.plusMinutes(30));
        cita.setDuracionMinutos(30);
        cita.setEstado(EstadoCita.COMPLETADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(new BigDecimal("100.00"));
        cita.setEliminada(false);
        cita.setEsEmergencia(false);
        cita = citaRepository.save(cita);

        Purchase purchase = new Purchase();
        purchase.setCompany(clinica);
        purchase.setCita(cita);
        purchase.setCliente(cliente);
        purchase.setUser(cliente);
        purchase.setTipoPurchase(TipoPurchase.SERVICIO_CITA);
        purchase.setPaymentStatus(PaymentStatus.PAID);
        purchase.setMetodoPago(MetodoPago.YAPE);
        purchase.setTotal(new BigDecimal("100.00"));
        purchase.setCreatedAt(fecha);
        return purchaseRepository.save(purchase);
    }

    private Usuario usuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setEmail("u-" + UUID.randomUUID() + "@pagos.test");
        usuario.setUsername("u-" + UUID.randomUUID());
        usuario.setNombre(nombre);
        usuario.setApellido("Apellido");
        usuario.setDni(digitos(8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(clinica);
        return usuarioRepository.save(usuario);
    }

    private String digitos(int largo) {
        String digitos = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        while (digitos.length() < largo) {
            digitos += "0";
        }
        return digitos.substring(0, largo);
    }
}
