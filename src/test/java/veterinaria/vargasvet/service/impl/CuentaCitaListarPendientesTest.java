package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.DetalleCuentaCita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;
import veterinaria.vargasvet.dto.response.CuentaCitaResponse;
import veterinaria.vargasvet.dto.response.DetalleCuentaResponse;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.DetalleCuentaCitaRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.InventarioStockService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaCitaListarPendientesTest {

    private static final Integer EMPRESA = 7;
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 7, 9, 0);

    @Mock private CitaRepository citaRepository;
    @Mock private DetalleCuentaCitaRepository detalleRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private AccesoValidator accesoValidator;
    @Mock private InventarioStockService inventarioStockService;

    @InjectMocks private CuentaCitaServiceImpl service;

    @BeforeEach
    void autenticarAdministradorDeLaClinica() {
        List<SimpleGrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "admin@clinica.test", "", autoridades, EMPRESA);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null, autoridades));
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void losDetallesDeTodasLasCuentasDeLaPaginaSeLeenConUnaSolaConsulta() {
        List<Cita> citas = List.of(cita(1L, "CIT-1"), cita(2L, "CIT-2"), cita(3L, "CIT-3"));
        paginaDeCitas(citas);
        when(detalleRepository.findDeLasCitas(List.of(1L, 2L, 3L))).thenReturn(List.of());

        service.listarPendientes(EMPRESA, 0, 20);

        verify(detalleRepository, times(1)).findDeLasCitas(List.of(1L, 2L, 3L));
        verify(detalleRepository, never()).findByCitaIdOrderByCreatedAtAscIdAsc(anyLong());
    }

    @Test
    void cadaCuentaRecibeSoloSusDetallesEnElOrdenEnQueLlegaron() {
        Cita primera = cita(1L, "CIT-1");
        Cita segunda = cita(2L, "CIT-2");
        paginaDeCitas(List.of(primera, segunda));
        when(detalleRepository.findDeLasCitas(List.of(1L, 2L))).thenReturn(List.of(
                detalle(10L, primera, "Consulta"),
                detalle(20L, segunda, "Vacuna"),
                detalle(11L, primera, "Insumo")));

        Page<CuentaCitaResponse> cuentas = service.listarPendientes(EMPRESA, 0, 20);

        assertThat(cuentas.getContent()).extracting(CuentaCitaResponse::getCitaId).containsExactly(1L, 2L);
        assertThat(cuentas.getContent().get(0).getDetalles()).extracting(DetalleCuentaResponse::getId).containsExactly(10L, 11L);
        assertThat(cuentas.getContent().get(1).getDetalles()).extracting(DetalleCuentaResponse::getId).containsExactly(20L);
    }

    @Test
    void unaCuentaSinDetallesApareceConUnaListaVacia() {
        Cita conDetalle = cita(1L, "CIT-1");
        Cita sinDetalle = cita(2L, "CIT-2");
        paginaDeCitas(List.of(conDetalle, sinDetalle));
        when(detalleRepository.findDeLasCitas(List.of(1L, 2L))).thenReturn(List.of(detalle(10L, conDetalle, "Consulta")));

        Page<CuentaCitaResponse> cuentas = service.listarPendientes(EMPRESA, 0, 20);

        assertThat(cuentas.getContent().get(1).getDetalles()).isEmpty();
    }

    @Test
    void unaPaginaVaciaNoConsultaLosDetalles() {
        paginaDeCitas(List.of());

        Page<CuentaCitaResponse> cuentas = service.listarPendientes(EMPRESA, 0, 20);

        assertThat(cuentas.getContent()).isEmpty();
        verify(detalleRepository, never()).findDeLasCitas(any());
    }

    @Test
    void armaLaCuentaConLosDatosDeLaCitaYSuSaldo() {
        Cita cita = cita(1L, "CIT-1");
        cita.setTotalServicio(new BigDecimal("150.00"));
        cita.setMontoPagado(new BigDecimal("40.00"));
        paginaDeCitas(List.of(cita));
        when(detalleRepository.findDeLasCitas(List.of(1L))).thenReturn(List.of());

        CuentaCitaResponse cuenta = service.listarPendientes(EMPRESA, 0, 20).getContent().getFirst();

        assertThat(cuenta.getNumeroCita()).isEqualTo("CIT-1");
        assertThat(cuenta.getMascotaNombre()).isEqualTo("Firulais");
        assertThat(cuenta.getApoderadoNombre()).isEqualTo("Ana Torres");
        assertThat(cuenta.getTotal()).isEqualByComparingTo("150.00");
        assertThat(cuenta.getSaldoPendiente()).isEqualByComparingTo("110.00");
        assertThat(cuenta.getEstadoPago()).isEqualTo("PAGO_PARCIAL");
        assertThat(cuenta.getCompanyId()).isEqualTo(EMPRESA);
    }

    @Test
    void unaCitaSinNumeroRecibeUnoYSeGuarda() {
        Cita sinNumero = cita(1L, null);
        paginaDeCitas(List.of(sinNumero));
        when(detalleRepository.findDeLasCitas(List.of(1L))).thenReturn(List.of());

        CuentaCitaResponse cuenta = service.listarPendientes(EMPRESA, 0, 20).getContent().getFirst();

        assertThat(cuenta.getNumeroCita()).startsWith("CIT-20261207-");
        verify(citaRepository).save(sinNumero);
    }

    private void paginaDeCitas(List<Cita> citas) {
        when(citaRepository.findCuentasPendientes(EMPRESA, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(citas, PageRequest.of(0, 20), citas.size()));
    }

    private Cita cita(Long id, String numero) {
        Company empresa = new Company();
        empresa.setId(EMPRESA);
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Firulais");
        mascota.setEspecie(EspecieMascota.PERRO);
        mascota.setApoderado(apoderado);

        Cita cita = new Cita();
        cita.setId(id);
        cita.setNumeroCita(numero);
        cita.setMascota(mascota);
        cita.setMotivoCita("Control");
        cita.setFechaHoraInicio(INICIO);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setTotalServicio(new BigDecimal("100.00"));
        cita.setMontoPagado(BigDecimal.ZERO);
        return cita;
    }

    private DetalleCuentaCita detalle(Long id, Cita cita, String descripcion) {
        DetalleCuentaCita detalle = new DetalleCuentaCita();
        detalle.setId(id);
        detalle.setCita(cita);
        detalle.setTipo(TipoDetalleCuenta.SERVICIO);
        detalle.setDescripcion(descripcion);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("50.00"));
        detalle.setSubtotal(new BigDecimal("50.00"));
        return detalle;
    }
}
