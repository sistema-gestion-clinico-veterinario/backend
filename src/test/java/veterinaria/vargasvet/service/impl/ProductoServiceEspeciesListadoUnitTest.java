package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.dto.response.ProductoResponse;
import veterinaria.vargasvet.repository.CategoriaProductoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MarcaProductoRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.UnidadMedidaRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.AuditLogService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La colección producto_especie es lazy: al mapear listados debe cargarse por lote
 * (una consulta por página) y jamás con un SELECT por producto.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceEspeciesListadoUnitTest {

    private static final Integer COMPANY_ID = 3;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CategoriaProductoRepository categoriaProductoRepository;

    @Mock
    private UnidadMedidaRepository unidadMedidaRepository;

    @Mock
    private MarcaProductoRepository marcaProductoRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProductoServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listarActivosCargaLasEspeciesDeTodosLosProductosEnUnaSolaConsulta() {
        autenticarConSede();
        Producto perro = productoMock(1L);
        Producto ave = productoMock(2L);
        when(productoRepository.findByCompanyIdAndActivoTrue(COMPANY_ID)).thenReturn(List.of(perro, ave));
        when(productoRepository.findEspeciesPorProductoIds(List.of(1L, 2L))).thenReturn(List.of(
                new Object[]{1L, "PERRO"},
                new Object[]{1L, "GATO"},
                new Object[]{2L, "AVE"}));
        when(productoRepository.findProximoVencimientoPorProducto(List.of(1L, 2L))).thenReturn(List.of());

        List<ProductoResponse> respuesta = service.listarActivos(COMPANY_ID);

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta.get(0).getEspecies())
                .containsExactlyInAnyOrder(EspecieMascota.PERRO, EspecieMascota.GATO);
        assertThat(respuesta.get(1).getEspecies()).containsExactly(EspecieMascota.AVE);

        // una única consulta por lote con todos los IDs, nunca una por producto
        verify(productoRepository, times(1)).findEspeciesPorProductoIds(List.of(1L, 2L));
        // la colección lazy jamás se toca al mapear un listado
        verify(perro, never()).getEspecies();
        verify(ave, never()).getEspecies();
    }

    @Test
    void obtenerProductoIncluyeSusEspeciesSinConsultarElLote() {
        autenticarConSede();
        EmpresaYProducto entity = productoConEspecies("PRD-TEST0001", EspecieMascota.PERRO, EspecieMascota.GATO);
        when(productoRepository.findByCompanyIdAndSku(COMPANY_ID, "PRD-TEST0001"))
                .thenReturn(Optional.of(entity.producto));
        when(productoRepository.findProximoVencimientoPorProducto(List.of(entity.producto.getId())))
                .thenReturn(List.of());

        ProductoResponse response = service.obtener("PRD-TEST0001", null);

        assertThat(response.getAplicacionEspecie()).isEqualTo(TipoAplicacionProducto.ESPECIES_ESPECIFICAS);
        assertThat(response.getEspecies())
                .containsExactlyInAnyOrder(EspecieMascota.PERRO, EspecieMascota.GATO);
        verify(productoRepository, never()).findEspeciesPorProductoIds(any());
    }

    // ---------------------------------------------------------------- helpers

    private Producto productoMock(Long id) {
        Producto producto = mock(Producto.class);
        when(producto.getId()).thenReturn(id);
        return producto;
    }

    private record EmpresaYProducto(Producto producto) {
    }

    private EmpresaYProducto productoConEspecies(String sku, EspecieMascota... especies) {
        Company company = new Company();
        company.setId(COMPANY_ID);

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCompany(company);
        producto.setNombre("Antiparasitario perros y gatos");
        producto.setSku(sku);
        producto.setActivo(true);
        producto.setAplicacionEspecie(TipoAplicacionProducto.ESPECIES_ESPECIFICAS);
        producto.setEspecies(new HashSet<>(Set.of(especies)));
        return new EmpresaYProducto(producto);
    }

    private void autenticarConSede() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UsuarioPrincipal principal = new UsuarioPrincipal(
                1, "admin@vargasvet.test", "", authorities, COMPANY_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null, authorities));
    }
}
