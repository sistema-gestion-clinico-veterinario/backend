package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.request.ReporteVentasProductosFiltro;
import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReporteVentasProductosDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.service.ReportesClinicosService;
import veterinaria.vargasvet.service.reportes.AlcanceReportes;
import veterinaria.vargasvet.service.reportes.RangoDeReporte;
import veterinaria.vargasvet.service.reportes.ReporteActividadClinica;
import veterinaria.vargasvet.service.reportes.ReporteComparativoEmpresas;
import veterinaria.vargasvet.service.reportes.ReporteIngresos;
import veterinaria.vargasvet.service.reportes.ReportePacientesInactivos;
import veterinaria.vargasvet.service.reportes.ReportePreventivos;
import veterinaria.vargasvet.service.reportes.ReporteVentasProductos;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportesClinicosServiceImpl implements ReportesClinicosService {

    private final AlcanceReportes alcance;
    private final CitaRepository citaRepository;
    private final ReporteActividadClinica actividad;
    private final ReporteIngresos ingresos;
    private final ReportePreventivos preventivos;
    private final ReportePacientesInactivos pacientesInactivos;
    private final ReporteVentasProductos ventasProductos;
    private final ReporteComparativoEmpresas comparativoEmpresas;

    @Override
    public ReportesClinicosDTO obtenerReportes(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta,
                                               Long veterinarioId, EspecieMascota especie) {
        Integer empresa = alcance.resolverEmpresa(companyId);
        Long veterinario = alcance.resolverVeterinario(veterinarioId);
        LocalDate hoy = AppClock.today();
        RangoDeReporte rango = RangoDeReporte.validado(fechaDesde, fechaHasta, hoy);
        if (empresa == null) {
            return reporteVacio(rango);
        }

        RangoDeReporte anterior = rango.anterior();
        List<Cita> actuales = citaRepository.findForClinicalReport(
                empresa, rango.inicio(), rango.finExclusivo(), veterinario, especie);
        List<Cita> anteriores = citaRepository.findForClinicalReport(
                empresa, anterior.inicio(), anterior.finExclusivo(), veterinario, especie);
        List<Mascota> pacientes = actividad.pacientesDe(actuales);

        // VISTA_CITAS y VISTA_CONTROL_PREVENTIVO eran códigos que ya no existen en el catálogo de
        // vistas (el primero fue reemplazado por VISTA_CITAS_AGENDA, el segundo nunca llegó a
        // sembrarse) — como el editor de roles no deja conceder un código inactivo/inexistente,
        // estas secciones quedaban ocultas para todos los roles salvo Admin/SuperAdmin.
        boolean puedeCitas = alcance.tienePermiso("VISTA_CITAS_AGENDA");
        boolean puedePagos = alcance.tienePermiso("VISTA_PAGOS");
        boolean puedeMascotas = alcance.tienePermiso("VISTA_MASCOTAS");
        boolean puedeControlPreventivo = alcance.tienePermiso("VISTA_CARTILLA");
        boolean alcanceEmpresa = alcance.tieneAlcanceEmpresa();
        // Estas métricas no tienen un responsable individual inequívoco. Con alcance
        // OWN se omiten en lugar de exponer datos agregados de toda la clínica.
        boolean verPreventivos = puedeControlPreventivo && alcanceEmpresa;

        return ReportesClinicosDTO.builder()
                .fechaDesde(rango.desde().toString())
                .fechaHasta(rango.hasta().toString())
                .resumen(actividad.resumen(actuales, rango, puedePagos))
                .resumenAnterior(actividad.resumen(anteriores, anterior, puedePagos))
                .consultasPorTipo(puedeCitas ? actividad.consultasPorTipo(actuales) : null)
                .consultasPorEstado(puedeCitas ? actividad.consultasPorEstado(actuales) : null)
                .pacientesPorEspecie(puedeMascotas ? actividad.pacientesPorEspecie(pacientes) : null)
                .pacientesPorRangoEdad(puedeMascotas ? actividad.pacientesPorRangoEdad(pacientes, rango.hasta()) : null)
                .consultasPorMes(puedeCitas ? actividad.consultasPorPeriodo(actuales, rango) : null)
                .consultasPorVeterinario(puedeCitas ? actividad.consultasPorVeterinario(actuales) : null)
                .frecuenciaConsultasPorPaciente(puedeCitas ? actividad.frecuenciaPorPaciente(actuales) : null)
                .serviciosMasSolicitados(puedeCitas ? actividad.serviciosMasSolicitados(actuales) : null)
                .demandaPorHorario(puedeCitas ? actividad.demandaPorHorario(actuales) : null)
                .proximasVacunas(verPreventivos ? preventivos.proximasVacunas(empresa) : null)
                .proximasDesparasitaciones(verPreventivos ? preventivos.proximasDesparasitaciones(empresa) : null)
                .controlesPreventivosProximos(verPreventivos ? preventivos.controlesProximos(empresa) : null)
                .ingresosPorMetodoPago(puedePagos ? ingresos.porMetodoDePago(actuales) : null)
                .ingresosPorServicio(puedePagos ? ingresos.porServicio(actuales) : null)
                .cumplimientoVacunacion(verPreventivos
                        ? preventivos.cumplimiento(empresa, TipoControlPreventivo.VACUNACION) : null)
                .cumplimientoDesparasitacion(verPreventivos
                        ? preventivos.cumplimiento(empresa, TipoControlPreventivo.DESPARASITACION) : null)
                .pacientesFrecuentes(puedeCitas ? actividad.pacientesFrecuentes(actuales) : null)
                .vacunasMasAplicadas(verPreventivos ? preventivos.vacunasMasAplicadas(empresa, rango) : null)
                .desparasitantesMasAplicados(verPreventivos ? preventivos.desparasitantesMasAplicados(empresa, rango) : null)
                .pacientesInactivos(puedeMascotas && puedeCitas && alcanceEmpresa
                        ? pacientesInactivos.listaCompleta(empresa, hoy) : null)
                .build();
    }

    @Override
    public ReportesComparativoEmpresasDTO obtenerComparativoEmpresas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                                      EspecieMascota especie) {
        return comparativoEmpresas.obtener(fechaDesde, fechaHasta, especie);
    }

    @Override
    public PacientesInactivosPageDTO obtenerPacientesInactivos(Integer companyId, int page, int size) {
        return pacientesInactivos.pagina(companyId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteVentasProductosDTO obtenerVentasProductos(ReporteVentasProductosFiltro filtros) {
        return ventasProductos.obtener(filtros);
    }

    private ReportesClinicosDTO reporteVacio(RangoDeReporte rango) {
        ReportesClinicosDTO.Resumen cero = ReportesClinicosDTO.Resumen.builder()
                .consultas(0).pacientesAtendidos(0).ingresos(BigDecimal.ZERO)
                .nuevosPacientes(0).tiempoPromedioAtencionMinutos(0)
                .porcentajeCitasCompletadas(0).noAsistieron(0).build();
        return ReportesClinicosDTO.builder()
                .fechaDesde(rango.desde().toString()).fechaHasta(rango.hasta().toString())
                .resumen(cero).resumenAnterior(cero)
                .consultasPorTipo(List.of()).consultasPorEstado(List.of())
                .pacientesPorEspecie(List.of()).pacientesPorRangoEdad(List.of())
                .proximasVacunas(List.of()).proximasDesparasitaciones(List.of())
                .consultasPorMes(List.of()).consultasPorVeterinario(List.of())
                .frecuenciaConsultasPorPaciente(List.of()).controlesPreventivosProximos(List.of())
                .serviciosMasSolicitados(List.of()).demandaPorHorario(List.of())
                .ingresosPorMetodoPago(List.of()).ingresosPorServicio(List.of())
                .cumplimientoVacunacion(List.of()).cumplimientoDesparasitacion(List.of())
                .pacientesFrecuentes(List.of()).vacunasMasAplicadas(List.of())
                .desparasitantesMasAplicados(List.of()).pacientesInactivos(List.of())
                .build();
    }
}
