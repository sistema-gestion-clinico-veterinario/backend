package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.dto.request.MovimientoEgresoRequest;
import veterinaria.vargasvet.dto.response.MovimientoCajaResponse;
import veterinaria.vargasvet.dto.response.ResumenCajaResponse;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.dto.request.AperturaCajaRequest;
import veterinaria.vargasvet.dto.request.ArqueoCajaRequest;
import veterinaria.vargasvet.dto.response.SesionCajaResponse;

import java.time.LocalDate;

public interface CajaService {

    void registrarIngresoPorCita(Cita cita, Integer companyId, java.math.BigDecimal monto, MetodoPago metodoPago);

    void registrarIngresoPorVentaLibre(Integer companyId, java.math.BigDecimal monto, MetodoPago metodoPago, String descripcion);

    MovimientoCajaResponse registrarDevolucion(Long citaId);

    MovimientoCajaResponse registrarEgreso(MovimientoEgresoRequest request);

    ResumenCajaResponse getResumen(Integer companyId, LocalDate desde, LocalDate hasta);

    Page<MovimientoCajaResponse> listar(Integer companyId, LocalDate desde, LocalDate hasta, int page, int size);

    SesionCajaResponse obtenerSesionActual(Integer companyId);

    Page<SesionCajaResponse> listarSesiones(Integer companyId, int page, int size);

    SesionCajaResponse abrirCaja(AperturaCajaRequest request);

    SesionCajaResponse arquearCaja(ArqueoCajaRequest request);

    SesionCajaResponse cerrarCaja(ArqueoCajaRequest request);
}
