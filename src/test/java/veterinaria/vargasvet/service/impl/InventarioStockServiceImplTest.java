package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Lote;
import veterinaria.vargasvet.domain.entity.Producto;
import veterinaria.vargasvet.domain.entity.SalidaLote;
import veterinaria.vargasvet.domain.enums.TipoControlStock;
import veterinaria.vargasvet.repository.LoteRepository;
import veterinaria.vargasvet.repository.ProductoRepository;
import veterinaria.vargasvet.repository.SalidaLoteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioStockServiceImplTest {

    private static final Pageable TRAMO = PageRequest.of(0, 5);

    @Mock ProductoRepository productoRepository;
    @Mock LoteRepository loteRepository;
    @Mock SalidaLoteRepository salidaLoteRepository;
    @InjectMocks InventarioStockServiceImpl service;

    @Test
    void descuentaPorFefoYRegistraCadaLoteUtilizado() {
        Producto producto = productoPorLotes();
        Lote primero = lote(11L, "L-PRIMERO", 3, LocalDate.now().plusMonths(1), producto);
        Lote segundo = lote(12L, "L-SEGUNDO", 8, LocalDate.now().plusMonths(3), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class), eq(TRAMO)))
                .thenReturn(List.of(primero, segundo));

        service.descontar(producto, 5, "VENTA_LIBRE_DETALLE", 99L);

        assertThat(primero.getCantidad()).isZero();
        assertThat(segundo.getCantidad()).isEqualTo(6);
        ArgumentCaptor<SalidaLote> captor = ArgumentCaptor.forClass(SalidaLote.class);
        verify(salidaLoteRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SalidaLote::getCantidad).containsExactly(3, 2);
        assertThat(captor.getAllValues()).allSatisfy(salida -> {
            assertThat(salida.getReferenciaTipo()).isEqualTo("VENTA_LIBRE_DETALLE");
            assertThat(salida.getReferenciaId()).isEqualTo(99L);
        });
        verify(productoRepository, never()).descontarStock(any(), any());
    }

    @Test
    void siElPrimerTramoAlcanzaNoLeeMasLotes() {
        Producto producto = productoPorLotes();
        Lote primero = lote(11L, "L-PRIMERO", 10, LocalDate.now().plusMonths(1), producto);
        Lote segundo = lote(12L, "L-SEGUNDO", 10, LocalDate.now().plusMonths(3), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class), eq(TRAMO)))
                .thenReturn(List.of(primero, segundo));

        service.descontar(producto, 4, "VENTA_LIBRE_DETALLE", 99L);

        verify(loteRepository, times(1)).findDisponiblesFefoForUpdate(any(), any(), any());
        verify(loteRepository, never()).flush();
        assertThat(primero.getCantidad()).isEqualTo(6);
        assertThat(segundo.getCantidad()).isEqualTo(10);
        verify(salidaLoteRepository, times(1)).save(any(SalidaLote.class));
    }

    @Test
    void sinoAlcanzaElPrimerTramoLeeElSiguienteDespuesDeGuardarLosCambios() {
        Producto producto = productoPorLotes();
        List<Lote> primerTramo = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            primerTramo.add(lote(20L + i, "L-" + i, 2, LocalDate.now().plusDays(10 + i), producto));
        }
        Lote siguiente = lote(30L, "L-SIGUIENTE", 9, LocalDate.now().plusDays(40), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class), eq(TRAMO)))
                .thenReturn(primerTramo)
                .thenReturn(List.of(siguiente));

        service.descontar(producto, 12, "VENTA_LIBRE_DETALLE", 99L);

        assertThat(primerTramo).extracting(Lote::getCantidad).containsOnly(0);
        assertThat(siguiente.getCantidad()).isEqualTo(7);
        InOrder orden = inOrder(loteRepository);
        orden.verify(loteRepository).findDisponiblesFefoForUpdate(any(), any(), any());
        orden.verify(loteRepository).flush();
        orden.verify(loteRepository).findDisponiblesFefoForUpdate(any(), any(), any());
        verify(loteRepository, times(1)).flush();
        ArgumentCaptor<SalidaLote> salidas = ArgumentCaptor.forClass(SalidaLote.class);
        verify(salidaLoteRepository, times(6)).save(salidas.capture());
        assertThat(salidas.getAllValues()).extracting(SalidaLote::getCantidad).containsExactly(2, 2, 2, 2, 2, 2);
    }

    @Test
    void lanzaElErrorDeStockInsuficienteCuandoYaNoQuedanLotesDisponibles() {
        Producto producto = productoPorLotes();
        Lote lote = lote(11L, "L-UNICO", 2, LocalDate.now().plusMonths(1), producto);
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class), eq(TRAMO)))
                .thenReturn(List.of(lote))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.descontar(producto, 3, "VENTA_LIBRE_DETALLE", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock por lotes insuficiente para «Vacuna»");
    }

    @Test
    void unProductoSinLotesDisponiblesLanzaElErrorSinTocarNada() {
        Producto producto = productoPorLotes();
        when(loteRepository.findDisponiblesFefoForUpdate(eq(10L), any(LocalDate.class), eq(TRAMO)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.descontar(producto, 1, "VENTA_LIBRE_DETALLE", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock por lotes insuficiente");

        verify(loteRepository, never()).save(any());
        verify(salidaLoteRepository, never()).save(any());
    }

    @Test
    void descontarCeroUnidadesNoLeeNiBloqueaNingunLote() {
        service.descontar(productoPorLotes(), 0, "VENTA_LIBRE_DETALLE", 99L);

        verify(loteRepository, never()).findDisponiblesFefoForUpdate(any(), any(), any());
    }

    @Test
    void unProductoSinControlPorLotesDescuentaElStockDirecto() {
        Producto producto = productoPorLotes();
        producto.setControlStock(TipoControlStock.DIRECTO);
        when(productoRepository.descontarStock(10L, 3)).thenReturn(1);

        service.descontar(producto, 3, "VENTA_LIBRE_DETALLE", 99L);

        verify(loteRepository, never()).findDisponiblesFefoForUpdate(any(), any(), any());
    }

    @Test
    void unProductoDirectoSinStockSuficienteLanzaElError() {
        Producto producto = productoPorLotes();
        producto.setControlStock(TipoControlStock.DIRECTO);
        when(productoRepository.descontarStock(10L, 3)).thenReturn(0);

        assertThatThrownBy(() -> service.descontar(producto, 3, "VENTA_LIBRE_DETALLE", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock insuficiente para «Vacuna»");
    }

    private Producto productoPorLotes() {
        Company company = new Company();
        company.setId(2);
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setCompany(company);
        producto.setNombre("Vacuna");
        producto.setControlStock(TipoControlStock.LOTES);
        return producto;
    }

    private Lote lote(Long id, String numero, int cantidad, LocalDate vencimiento, Producto producto) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setProducto(producto);
        lote.setCompany(producto.getCompany());
        lote.setNumeroLote(numero);
        lote.setCantidad(cantidad);
        lote.setCantidadInicial(cantidad);
        lote.setFechaVencimiento(vencimiento);
        lote.setActivo(true);
        return lote;
    }
}
