package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;
import veterinaria.vargasvet.dto.request.DetalleCuentaRequest;
import veterinaria.vargasvet.dto.response.CuentaCitaResponse;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.DetalleCuentaCitaRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validación de aplicación por especie cuando la venta queda vinculada a la mascota de una cita.
 */
@ExtendWith(MockitoExtension.class)
class CuentaCitaEspecieProductoUnitTest {

    private static final Long CITA_ID = 1L;
    private static final Long PRODUCTO_ID = 10L;
    private static final Integer COMPANY_ID = 3;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private DetalleCuentaCitaRepository detalleRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CuentaCitaServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productoDeUsoGeneralSeAgregaSinJustificacionNiAuditoria() {
        autenticar(COMPANY_ID, "ROLE_VETERINARIO");
        prepararCita(EspecieMascota.GATO);
        prepararProducto(producto(TipoAplicacionProducto.USO_GENERAL, Set.of()));
        prepararStockYGuardadoExitoso();

        CuentaCitaResponse response = assertDoesNotThrow(() -> service.agregarDetalle(CITA_ID, request()));

        assertNotNull(response);
        verify(productoRepository).descontarStock(PRODUCTO_ID, 1);
        verify(auditLogService, never()).log(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void productoSinClasificarLegadoNoBloqueaLaVenta() {
        autenticar(COMPANY_ID, "ROLE_ADMIN");
        prepararCita(EspecieMascota.REPTIL);
        prepararProducto(producto(TipoAplicacionProducto.NO_ESPECIFICADO, Set.of()));
        prepararStockYGuardadoExitoso();

        assertNotNull(assertDoesNotThrow(() -> service.agregarDetalle(CITA_ID, request())));
        verify(productoRepository).descontarStock(PRODUCTO_ID, 1);
    }

    @Test
    void productoCompatibleConLaEspecieDeLaMascotaSeAgrega() {
        autenticar(COMPANY_ID, "ROLE_ADMIN");
        prepararCita(EspecieMascota.PERRO);
        prepararProducto(producto(TipoAplicacionProducto.ESPECIES_ESPECIFICAS,
                Set.of(EspecieMascota.PERRO, EspecieMascota.GATO)));
        prepararStockYGuardadoExitoso();

        assertNotNull(assertDoesNotThrow(() -> service.agregarDetalle(CITA_ID, request())));
        verify(productoRepository).descontarStock(PRODUCTO_ID, 1);
        verify(auditLogService, never()).log(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void productoDeOtraEspecieSinJustificacionSeBloqueaSinDescontarStock() {
        autenticar(COMPANY_ID, "ROLE_VETERINARIO");
        prepararCita(EspecieMascota.AVE);
        prepararProducto(producto(TipoAplicacionProducto.ESPECIES_ESPECIFICAS,
                Set.of(EspecieMascota.PERRO, EspecieMascota.GATO)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.agregarDetalle(CITA_ID, request()));

        assertTrue(ex.getMessage().contains("no corresponde a la especie"), ex.getMessage());
        assertTrue(ex.getMessage().contains("gatos"), ex.getMessage());
        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
        verify(auditLogService, never()).log(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void usoExcepcionalJustificadoLoRechazaSiElRolNoEsVeterinario() {
        autenticar(COMPANY_ID, "ROLE_ADMIN");
        prepararCita(EspecieMascota.AVE);
        prepararProducto(producto(TipoAplicacionProducto.ESPECIES_ESPECIFICAS,
                Set.of(EspecieMascota.PERRO, EspecieMascota.GATO)));
        DetalleCuentaRequest request = request();
        request.setJustificacionUsoExcepcional("Paciente con gusanera, dosis unica autorizada");

        assertThrows(AccessDeniedException.class, () -> service.agregarDetalle(CITA_ID, request));

        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
        verify(auditLogService, never()).log(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void usoExcepcionalJustificadoLoRegistraVeterinarioYDejaAuditoria() {
        autenticar(COMPANY_ID, "ROLE_VETERINARIO");
        prepararCita(EspecieMascota.AVE);
        prepararProducto(producto(TipoAplicacionProducto.ESPECIES_ESPECIFICAS,
                Set.of(EspecieMascota.PERRO, EspecieMascota.GATO)));
        DetalleCuentaRequest request = request();
        request.setJustificacionUsoExcepcional("  Paciente con gusanera, dosis unica autorizada  ");
        prepararStockYGuardadoExitoso();

        assertNotNull(assertDoesNotThrow(() -> service.agregarDetalle(CITA_ID, request)));

        verify(productoRepository).descontarStock(PRODUCTO_ID, 1);
        verify(auditLogService).log(
                eq(COMPANY_ID),
                eq("USO_EXCEPCIONAL_ESPECIE"),
                eq("Cuenta de cita"),
                contains("Justificación clínica: Paciente con gusanera"));
    }

    // ---------------------------------------------------------------- helpers

    private DetalleCuentaRequest request() {
        DetalleCuentaRequest request = new DetalleCuentaRequest();
        request.setTipo(TipoDetalleCuenta.MEDICAMENTO);
        request.setDescripcion("Antiparasitario");
        request.setCantidad(1);
        request.setPrecioUnitario(new BigDecimal("25.00"));
        request.setProductoId(PRODUCTO_ID);
        return request;
    }

    private void prepararCita(EspecieMascota especieMascota) {
        when(citaRepository.findById(CITA_ID)).thenReturn(Optional.of(cita(especieMascota)));
        when(detalleRepository.existsByCitaIdAndEsServicioBaseTrue(CITA_ID)).thenReturn(false);
    }

    private void prepararProducto(Producto producto) {
        when(productoRepository.findById(PRODUCTO_ID)).thenReturn(Optional.of(producto));
    }

    /** Stub usados solo cuando la validación de especie permite continuar. */
    private void prepararStockYGuardadoExitoso() {
        when(productoRepository.descontarStock(PRODUCTO_ID, 1)).thenReturn(1);
        when(detalleRepository.findByCitaIdOrderByCreatedAtAscIdAsc(CITA_ID)).thenReturn(List.of());
    }

    private Producto producto(TipoAplicacionProducto aplicacion, Set<EspecieMascota> especies) {
        Company company = new Company();
        company.setId(COMPANY_ID);

        Producto producto = new Producto();
        producto.setId(PRODUCTO_ID);
        producto.setCompany(company);
        producto.setNombre("Antiparasitario perros y gatos");
        producto.setSku("PRD-TEST0001");
        producto.setActivo(true);
        producto.setAplicacionEspecie(aplicacion);
        producto.setEspecies(new HashSet<>(especies));
        return producto;
    }

    private Cita cita(EspecieMascota especieMascota) {
        Company company = new Company();
        company.setId(COMPANY_ID);

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");

        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(company);

        Mascota mascota = new Mascota();
        mascota.setNombreCompleto("Rex");
        mascota.setEspecie(especieMascota);
        mascota.setApoderado(apoderado);

        Cita cita = new Cita();
        cita.setId(CITA_ID);
        cita.setMascota(mascota);
        cita.setEstado(EstadoCita.EN_PROCESO);
        return cita;
    }

    private void autenticar(Integer companyId, String rol) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(rol));
        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "usuario@vargasvet.test", "", authorities, companyId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities));
    }
}
