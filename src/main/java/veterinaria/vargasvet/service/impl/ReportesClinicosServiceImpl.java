package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Purchase;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;
import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.ControlPreventivoRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PurchaseRepository;
import veterinaria.vargasvet.repository.RegistroDesparasitacionRepository;
import veterinaria.vargasvet.repository.RegistroVacunaRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ReportesClinicosService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportesClinicosServiceImpl implements ReportesClinicosService {

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final RegistroVacunaRepository registroVacunaRepository;
    private final RegistroDesparasitacionRepository registroDesparasitacionRepository;
    private final ControlPreventivoRepository controlPreventivoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PurchaseRepository purchaseRepository;
    private final AccesoValidator accesoValidator;
    private final CompanyRepository companyRepository;

    @Override
    public ReportesClinicosDTO obtenerReportes(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta,
                                               Long veterinarioId, EspecieMascota especie) {
        Integer targetCompanyId = resolveCompanyId(companyId);
        Long targetVeterinarioId = resolveVeterinarioId(veterinarioId);
        LocalDate hoy = AppClock.today();
        LocalDate desde = fechaDesde != null ? fechaDesde : hoy.withDayOfMonth(1);
        LocalDate hasta = fechaHasta != null ? fechaHasta : hoy;
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > 1110) {
            throw new IllegalArgumentException("El rango máximo permitido para reportes es de 36 meses");
        }

        if (targetCompanyId == null) {
            return emptyReport(desde, hasta);
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate anteriorHasta = desde.minusDays(1);
        LocalDate anteriorDesde = anteriorHasta.minusDays(dias - 1);

        List<Cita> actuales = citaRepository.findForClinicalReport(
                targetCompanyId, inicio, finExclusivo, targetVeterinarioId, especie);
        List<Cita> anteriores = citaRepository.findForClinicalReport(
                targetCompanyId, anteriorDesde.atStartOfDay(), desde.atStartOfDay(), targetVeterinarioId, especie);

        List<Mascota> pacientes = new ArrayList<>(actuales.stream()
                .map(Cita::getMascota)
                .collect(Collectors.toMap(
                        Mascota::getId,
                        Function.identity(),
                        (primera, repetida) -> primera,
                        LinkedHashMap::new))
                .values());

        // VISTA_CITAS y VISTA_CONTROL_PREVENTIVO eran códigos que ya no existen en el catálogo de
        // vistas (el primero fue reemplazado por VISTA_CITAS_AGENDA, el segundo nunca llegó a
        // sembrarse) — como el editor de roles no deja conceder un código inactivo/inexistente,
        // estas secciones quedaban ocultas para todos los roles salvo Admin/SuperAdmin.
        boolean puedeCitas = tienePermiso("VISTA_CITAS_AGENDA");
        boolean puedePagos = tienePermiso("VISTA_PAGOS");
        boolean puedeMascotas = tienePermiso("VISTA_MASCOTAS");
        boolean puedeControlPreventivo = tienePermiso("VISTA_CARTILLA");

        return ReportesClinicosDTO.builder()
                .fechaDesde(desde.toString())
                .fechaHasta(hasta.toString())
                .resumen(calcularResumen(actuales, desde, hasta, puedePagos))
                .resumenAnterior(calcularResumen(anteriores, anteriorDesde, anteriorHasta, puedePagos))
                .consultasPorTipo(puedeCitas ? group(actuales, this::tipoConsultaLabel) : null)
                .consultasPorEstado(puedeCitas ? group(actuales, c -> humanize(c.getEstado().name())) : null)
                .pacientesPorEspecie(puedeMascotas ? groupMascotas(pacientes, m -> humanize(m.getEspecie().name())) : null)
                .pacientesPorRangoEdad(puedeMascotas ? calcularRangosEdad(pacientes, hasta) : null)
                .consultasPorMes(puedeCitas ? calcularSerie(actuales, desde, hasta) : null)
                .consultasPorVeterinario(puedeCitas ? group(actuales, this::nombreVeterinario) : null)
                .frecuenciaConsultasPorPaciente(puedeCitas ? calcularFrecuencia(actuales) : null)
                .serviciosMasSolicitados(puedeCitas ? group(actuales, this::servicioLabel) : null)
                .demandaPorHorario(puedeCitas ? calcularDemanda(actuales) : null)
                .proximasVacunas(puedeControlPreventivo ? findProximasVacunas(targetCompanyId) : null)
                .proximasDesparasitaciones(puedeControlPreventivo ? findProximasDesparasitaciones(targetCompanyId) : null)
                .controlesPreventivosProximos(puedeControlPreventivo ? findControlesPreventivosProximos(targetCompanyId) : null)
                .ingresosPorMetodoPago(puedePagos ? calcularIngresosPorMetodoPago(actuales) : null)
                .ingresosPorServicio(puedePagos ? calcularIngresosPorServicio(actuales) : null)
                .cumplimientoVacunacion(puedeControlPreventivo
                        ? calcularCumplimiento(targetCompanyId, TipoControlPreventivo.VACUNACION) : null)
                .cumplimientoDesparasitacion(puedeControlPreventivo
                        ? calcularCumplimiento(targetCompanyId, TipoControlPreventivo.DESPARASITACION) : null)
                .pacientesFrecuentes(puedeCitas ? calcularPacientesFrecuentes(actuales) : null)
                .vacunasMasAplicadas(puedeControlPreventivo
                        ? calcularVacunasMasAplicadas(targetCompanyId, desde, hasta) : null)
                .desparasitantesMasAplicados(puedeControlPreventivo
                        ? calcularDesparasitantesMasAplicados(targetCompanyId, desde, hasta) : null)
                .pacientesInactivos(puedeMascotas && puedeCitas
                        ? calcularPacientesInactivos(targetCompanyId, hoy) : null)
                .build();
    }

    @Override
    public ReportesComparativoEmpresasDTO obtenerComparativoEmpresas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                                      EspecieMascota especie) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("Solo la administración de plataforma puede comparar empresas");
        }
        LocalDate hoy = AppClock.today();
        LocalDate desde = fechaDesde != null ? fechaDesde : hoy.withDayOfMonth(1);
        LocalDate hasta = fechaHasta != null ? fechaHasta : hoy;
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > 1110) {
            throw new IllegalArgumentException("El rango máximo permitido para reportes es de 36 meses");
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();

        List<ReportesComparativoEmpresasDTO.EmpresaResumen> empresas = companyRepository.findAll().stream()
                .filter(Company::isActivo)
                .map(company -> {
                    List<Cita> citas = citaRepository.findForClinicalReport(
                            company.getId(), inicio, finExclusivo, null, especie);
                    ReportesClinicosDTO.Resumen resumen = calcularResumen(citas, desde, hasta, true);
                    return ReportesComparativoEmpresasDTO.EmpresaResumen.builder()
                            .companyId(company.getId())
                            .companyName(company.getName())
                            .consultas(resumen.getConsultas())
                            .pacientesAtendidos(resumen.getPacientesAtendidos())
                            .ingresos(resumen.getIngresos())
                            .nuevosPacientes(resumen.getNuevosPacientes())
                            .porcentajeCitasCompletadas(resumen.getPorcentajeCitasCompletadas())
                            .noAsistieron(resumen.getNoAsistieron())
                            .build();
                })
                .sorted(Comparator.comparingLong(ReportesComparativoEmpresasDTO.EmpresaResumen::getConsultas).reversed())
                .toList();

        return ReportesComparativoEmpresasDTO.builder()
                .fechaDesde(desde.toString())
                .fechaHasta(hasta.toString())
                .empresas(empresas)
                .build();
    }

    @Override
    public PacientesInactivosPageDTO obtenerPacientesInactivos(Integer companyId, int page, int size) {
        Integer targetCompanyId = resolveCompanyId(companyId);
        if (targetCompanyId == null) {
            return PacientesInactivosPageDTO.builder()
                    .content(List.of()).page(page).size(size).totalElements(0).totalPages(0).build();
        }
        int pageSize = Math.max(1, Math.min(size, 100));
        LocalDate hoy = AppClock.today();
        LocalDateTime umbral = hoy.minusMonths(3).atStartOfDay();

        org.springframework.data.domain.Page<veterinaria.vargasvet.domain.entity.Mascota> paginaMascotas =
                mascotaRepository.findInactivasByCompanyId(
                        targetCompanyId, umbral, org.springframework.data.domain.PageRequest.of(Math.max(0, page), pageSize));

        List<Long> mascotaIds = paginaMascotas.getContent().stream()
                .map(veterinaria.vargasvet.domain.entity.Mascota::getId)
                .toList();
        Map<Long, LocalDateTime> ultimaVisitaPorMascota = mascotaIds.isEmpty() ? Map.of()
                : citaRepository.findUltimaVisitaCompletadaByMascotaIds(mascotaIds).stream()
                        .collect(Collectors.toMap(
                                CitaRepository.UltimaVisitaPorMascota::getMascotaId,
                                CitaRepository.UltimaVisitaPorMascota::getFecha));

        List<ReportesClinicosDTO.PacienteInactivo> content = paginaMascotas.getContent().stream()
                .map(m -> {
                    LocalDateTime ultima = ultimaVisitaPorMascota.get(m.getId());
                    LocalDate ultimaFecha = ultima != null ? ultima.toLocalDate() : null;
                    LocalDate referencia = ultimaFecha != null ? ultimaFecha
                            : (m.getCreatedAt() != null ? m.getCreatedAt().toLocalDate() : hoy);
                    long dias = ChronoUnit.DAYS.between(referencia, hoy);
                    return ReportesClinicosDTO.PacienteInactivo.builder()
                            .mascota(m.getNombreCompleto())
                            .apoderado(m.getApoderado() != null && m.getApoderado().getUser() != null
                                    ? m.getApoderado().getUser().getNombre() + " " + m.getApoderado().getUser().getApellido()
                                    : "Sin propietario")
                            .ultimaVisita(ultimaFecha != null ? ultimaFecha.toString() : "Nunca visitó")
                            .diasSinVisitar(dias)
                            .build();
                })
                .toList();

        return PacientesInactivosPageDTO.builder()
                .content(content)
                .page(paginaMascotas.getNumber())
                .size(paginaMascotas.getSize())
                .totalElements(paginaMascotas.getTotalElements())
                .totalPages(paginaMascotas.getTotalPages())
                .build();
    }

    private boolean tienePermiso(String vista) {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin() || accesoValidator.can(vista, "LEER");
    }

    private Integer resolveCompanyId(Integer requestedCompanyId) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestedCompanyId;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    private Long resolveVeterinarioId(Long requestedVeterinarioId) {
        if (SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin()) {
            return requestedVeterinarioId;
        }
        if (accesoValidator.canAccessCompanyData("VISTA_REPORTES")) {
            return requestedVeterinarioId;
        }
        return empleadoRepository.findActiveByUserId(SecurityUtils.getCurrentUserId())
                .map(veterinaria.vargasvet.domain.entity.Empleado::getId)
                .orElse(-1L);
    }

    private ReportesClinicosDTO.Resumen calcularResumen(List<Cita> citas, LocalDate desde, LocalDate hasta, boolean puedePagos) {
        Set<Long> pacientesAtendidos = citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.COMPLETADA || c.getEstado() == EstadoCita.EN_PROCESO)
                .map(c -> c.getMascota().getId())
                .collect(Collectors.toSet());

        BigDecimal ingresos = citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .map(this::importeReal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long nuevosPacientes = citas.stream()
                .map(Cita::getMascota)
                .filter(m -> m.getCreatedAt() != null)
                .filter(m -> !m.getCreatedAt().toLocalDate().isBefore(desde)
                        && !m.getCreatedAt().toLocalDate().isAfter(hasta))
                .map(Mascota::getId)
                .distinct()
                .count();

        List<Long> duraciones = citas.stream()
                .filter(c -> c.getConsulta() != null)
                .filter(c -> c.getConsulta().getFechaConsulta() != null && c.getConsulta().getFechaCierre() != null)
                .map(c -> ChronoUnit.MINUTES.between(
                        c.getConsulta().getFechaConsulta(), c.getConsulta().getFechaCierre()))
                .filter(minutos -> minutos >= 0 && minutos <= 24 * 60)
                .toList();
        long promedio = duraciones.isEmpty()
                ? 0
                : Math.round(duraciones.stream().mapToLong(Long::longValue).average().orElse(0));

        long cerradas = citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.COMPLETADA
                        || c.getEstado() == EstadoCita.CANCELADA
                        || c.getEstado() == EstadoCita.NO_ASISTIO)
                .count();
        long completadas = citas.stream().filter(c -> c.getEstado() == EstadoCita.COMPLETADA).count();
        double porcentaje = cerradas == 0 ? 0 : (completadas * 100.0 / cerradas);
        long noAsistieron = citas.stream().filter(c -> c.getEstado() == EstadoCita.NO_ASISTIO).count();

        return ReportesClinicosDTO.Resumen.builder()
                .consultas(citas.size())
                .pacientesAtendidos(pacientesAtendidos.size())
                .ingresos(puedePagos ? ingresos.setScale(2, RoundingMode.HALF_UP) : null)
                .nuevosPacientes(nuevosPacientes)
                .tiempoPromedioAtencionMinutos(promedio)
                .porcentajeCitasCompletadas(Math.round(porcentaje * 10.0) / 10.0)
                .noAsistieron(noAsistieron)
                .build();
    }

    private BigDecimal importeReal(Cita cita) {
        BigDecimal pagado = cita.getMontoPagado() != null ? cita.getMontoPagado() : BigDecimal.ZERO;
        BigDecimal total = cita.getTotalServicio() != null ? cita.getTotalServicio() : BigDecimal.ZERO;
        return pagado.min(total).max(BigDecimal.ZERO);
    }

    private List<ReportesClinicosDTO.ItemMonto> calcularIngresosPorMetodoPago(List<Cita> citas) {
        List<Purchase> pagos = purchasesPagados(citas);
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        for (Purchase pago : pagos) {
            String metodo = pago.getMetodoPago() != null ? humanize(pago.getMetodoPago().name()) : "Sin especificar";
            montos.merge(metodo, montoPagoReal(pago), BigDecimal::add);
        }
        return montos.entrySet().stream()
                .map(e -> itemMonto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ReportesClinicosDTO.ItemMonto::getMonto).reversed())
                .toList();
    }

    private List<ReportesClinicosDTO.ItemMonto> calcularIngresosPorServicio(List<Cita> citas) {
        List<Purchase> pagos = purchasesPagados(citas);
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        for (Purchase pago : pagos) {
            String servicio = pago.getCita() != null ? servicioLabel(pago.getCita()) : "Sin servicio";
            montos.merge(servicio, montoPagoReal(pago), BigDecimal::add);
        }
        return montos.entrySet().stream()
                .map(e -> itemMonto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ReportesClinicosDTO.ItemMonto::getMonto).reversed())
                .toList();
    }

    private List<Purchase> purchasesPagados(List<Cita> citas) {
        List<Long> citaIds = citas.stream().map(Cita::getId).toList();
        if (citaIds.isEmpty()) return List.of();
        return purchaseRepository.findPagadosByCitaIds(citaIds);
    }

    private BigDecimal montoPagoReal(Purchase pago) {
        return pago.getTotal() != null ? pago.getTotal() : BigDecimal.ZERO;
    }

    private ReportesClinicosDTO.ItemMonto itemMonto(String label, BigDecimal monto) {
        return ReportesClinicosDTO.ItemMonto.builder()
                .label(label)
                .monto(monto.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularCumplimiento(Integer companyId, TipoControlPreventivo tipo) {
        List<EstadoControlPreventivo> estadosRelevantes = List.of(
                EstadoControlPreventivo.PROGRAMADO, EstadoControlPreventivo.PROXIMO,
                EstadoControlPreventivo.PENDIENTE, EstadoControlPreventivo.ATRASADO);

        List<ControlPreventivo> controles = controlPreventivoRepository
                .findPendientesByCompany(companyId, estadosRelevantes).stream()
                .filter(cp -> cp.getTipo() == tipo)
                .toList();

        Set<Long> mascotasAtrasadas = controles.stream()
                .filter(cp -> cp.getEstado() == EstadoControlPreventivo.ATRASADO)
                .map(cp -> cp.getMascota().getId())
                .collect(Collectors.toSet());
        Set<Long> mascotasConControl = controles.stream()
                .map(cp -> cp.getMascota().getId())
                .collect(Collectors.toCollection(HashSet::new));

        long alDia = mascotasConControl.size() - mascotasAtrasadas.size();
        long atrasados = mascotasAtrasadas.size();

        List<ReportesClinicosDTO.ItemCount> result = new ArrayList<>();
        if (alDia > 0) result.add(item("Al día", alDia));
        if (atrasados > 0) result.add(item("Atrasado", atrasados));
        return result;
    }

    private List<ReportesClinicosDTO.ItemCount> calcularSerie(
            List<Cita> citas, LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        Map<String, Long> values = new LinkedHashMap<>();
        Function<Cita, String> keyFn;

        if (dias == 1) {
            for (int hora = 7; hora <= 20; hora++) values.put(String.format("%02d:00", hora), 0L);
            keyFn = c -> String.format("%02d:00", c.getFechaHoraInicio().getHour());
        } else if (dias <= 45) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM", new Locale("es", "PE"));
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) values.put(d.format(formatter), 0L);
            keyFn = c -> c.getFechaHoraInicio().toLocalDate().format(formatter);
        } else {
            DateTimeFormatter key = DateTimeFormatter.ofPattern("yyyy-MM");
            DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yy", new Locale("es", "PE"));
            for (LocalDate d = desde.withDayOfMonth(1); !d.isAfter(hasta); d = d.plusMonths(1)) {
                values.put(d.format(key), 0L);
            }
            for (Cita cita : citas) values.merge(cita.getFechaHoraInicio().format(key), 1L, Long::sum);
            return values.entrySet().stream()
                    .map(e -> item(LocalDate.parse(e.getKey() + "-01").format(label), e.getValue()))
                    .toList();
        }

        for (Cita cita : citas) {
            String key = keyFn.apply(cita);
            if (values.containsKey(key)) values.merge(key, 1L, Long::sum);
        }
        return values.entrySet().stream().map(e -> item(e.getKey(), e.getValue())).toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularRangosEdad(List<Mascota> mascotas, LocalDate referencia) {
        Map<String, Long> rangos = new LinkedHashMap<>();
        rangos.put("Cachorro (0-1 año)", 0L);
        rangos.put("Joven (1-3 años)", 0L);
        rangos.put("Adulto (4-7 años)", 0L);
        rangos.put("Senior (8+ años)", 0L);
        rangos.put("Sin registro", 0L);

        for (Mascota mascota : mascotas) {
            if (mascota.getFechaNacimiento() == null) {
                rangos.merge("Sin registro", 1L, Long::sum);
                continue;
            }
            long edad = ChronoUnit.YEARS.between(mascota.getFechaNacimiento(), referencia);
            if (edad < 1) rangos.merge("Cachorro (0-1 año)", 1L, Long::sum);
            else if (edad <= 3) rangos.merge("Joven (1-3 años)", 1L, Long::sum);
            else if (edad <= 7) rangos.merge("Adulto (4-7 años)", 1L, Long::sum);
            else rangos.merge("Senior (8+ años)", 1L, Long::sum);
        }
        return rangos.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> item(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularFrecuencia(List<Cita> citas) {
        Map<Long, Long> porMascota = citas.stream()
                .collect(Collectors.groupingBy(c -> c.getMascota().getId(), Collectors.counting()));
        long una = porMascota.values().stream().filter(c -> c == 1).count();
        long dosTres = porMascota.values().stream().filter(c -> c >= 2 && c <= 3).count();
        long cuatroMas = porMascota.values().stream().filter(c -> c >= 4).count();
        List<ReportesClinicosDTO.ItemCount> result = new ArrayList<>();
        if (una > 0) result.add(item("1 cita", una));
        if (dosTres > 0) result.add(item("2-3 citas", dosTres));
        if (cuatroMas > 0) result.add(item("4+ citas", cuatroMas));
        return result;
    }

    private List<ReportesClinicosDTO.HeatmapItem> calcularDemanda(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getEstado() != EstadoCita.NO_ASISTIO)
                .collect(Collectors.groupingBy(c -> c.getFechaHoraInicio().getDayOfWeek().getValue()
                        + "-" + c.getFechaHoraInicio().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    String[] key = e.getKey().split("-");
                    return ReportesClinicosDTO.HeatmapItem.builder()
                            .diaSemana(Integer.parseInt(key[0]))
                            .hora(Integer.parseInt(key[1]))
                            .count(e.getValue())
                            .build();
                })
                .sorted(Comparator.comparingInt(ReportesClinicosDTO.HeatmapItem::getHora)
                        .thenComparingInt(ReportesClinicosDTO.HeatmapItem::getDiaSemana))
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> group(List<Cita> citas, Function<Cita, String> labelFn) {
        return citas.stream()
                .collect(Collectors.groupingBy(labelFn, Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> groupMascotas(
            List<Mascota> mascotas, Function<Mascota, String> labelFn) {
        return mascotas.stream()
                .collect(Collectors.groupingBy(labelFn, Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    private String tipoConsultaLabel(Cita cita) {
        return cita.getConsulta() != null && cita.getConsulta().getTipoConsulta() != null
                ? humanize(cita.getConsulta().getTipoConsulta().name())
                : servicioLabel(cita);
    }

    private String servicioLabel(Cita cita) {
        return cita.getServicio() != null && cita.getServicio().getNombre() != null
                ? cita.getServicio().getNombre()
                : "Sin servicio";
    }

    private String nombreVeterinario(Cita cita) {
        if (cita.getEmpleado() == null || cita.getEmpleado().getUser() == null) return "Sin asignar";
        String nombre = cita.getEmpleado().getUser().getNombre();
        String apellido = cita.getEmpleado().getUser().getApellido();
        return ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
    }

    private String humanize(String value) {
        if ("NO_ASISTIO".equals(value)) return "No asistió";
        String normalized = value.replace("_", " ").toLowerCase(new Locale("es", "PE"));
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private ReportesClinicosDTO.ItemCount item(String label, long count) {
        return ReportesClinicosDTO.ItemCount.builder().label(label).count(count).build();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasVacunas(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroVacunaRepository.findProximasVacunas(companyId, hoy, hoy.plusDays(30), PageRequest.of(0, 5)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getNombreVacuna())
                        .fechaProxima(r.getFechaProximaDosis().toString())
                        .tipoControl("VACUNACION")
                        .build())
                .toList();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findProximasDesparasitaciones(Integer companyId) {
        LocalDate hoy = AppClock.today();
        return registroDesparasitacionRepository.findProximasDesparasitaciones(
                        companyId, hoy, hoy.plusDays(30), PageRequest.of(0, 5)).stream()
                .map(r -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(r.getHistoriaClinica().getMascota().getNombreCompleto())
                        .producto(r.getProducto())
                        .fechaProxima(r.getFechaProximaAplicacion().toString())
                        .tipoControl("DESPARASITACION")
                        .build())
                .toList();
    }

    private List<ReportesClinicosDTO.ProximaAplicacion> findControlesPreventivosProximos(Integer companyId) {
        LocalDate hoy = AppClock.today();
        List<veterinaria.vargasvet.domain.enums.EstadoControlPreventivo> estados = List.of(
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROGRAMADO,
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROXIMO,
                veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PENDIENTE);
        return controlPreventivoRepository.findProximosByCompany(
                        companyId, hoy, hoy.plusDays(30), estados, PageRequest.of(0, 5)).stream()
                .map(cp -> ReportesClinicosDTO.ProximaAplicacion.builder()
                        .mascota(cp.getMascota().getNombreCompleto())
                        .producto(cp.getNombreControl())
                        .fechaProxima(cp.getFechaRecomendada().toString())
                        .tipoControl(cp.getTipo().name())
                        .build())
                .toList();
    }

    private static final int TOP_N_DEFAULT = 10;

    private List<ReportesClinicosDTO.ItemCount> calcularPacientesFrecuentes(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.COMPLETADA)
                .collect(Collectors.groupingBy(c -> c.getMascota().getNombreCompleto(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .limit(TOP_N_DEFAULT)
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularVacunasMasAplicadas(
            Integer companyId, LocalDate desde, LocalDate hasta) {
        return registroVacunaRepository.findAplicadasByCompanyAndFecha(companyId, desde, hasta).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getNombreVacuna() != null ? r.getNombreVacuna() : "Sin especificar",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    private List<ReportesClinicosDTO.ItemCount> calcularDesparasitantesMasAplicados(
            Integer companyId, LocalDate desde, LocalDate hasta) {
        return registroDesparasitacionRepository.findAplicadasByCompanyAndFecha(companyId, desde, hasta).stream()
                .collect(Collectors.groupingBy(
                        r -> r.getProducto() != null ? r.getProducto() : "Sin especificar",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.ItemCount::getCount).reversed())
                .toList();
    }

    /** Independiente del rango de fechas del panel de reportes: siempre mira el
     * historial completo de cada mascota activa para encontrar su ultima cita
     * COMPLETADA, sin importar que rango este seleccionado. Una mascota que nunca
     * tuvo una cita completada tambien cuenta como inactiva. */
    private List<ReportesClinicosDTO.PacienteInactivo> calcularPacientesInactivos(Integer companyId, LocalDate hoy) {
        LocalDate umbral = hoy.minusMonths(3);
        Map<Long, LocalDateTime> ultimaVisitaPorMascota = citaRepository.findUltimaVisitaCompletadaPorCompany(companyId)
                .stream()
                .collect(Collectors.toMap(
                        CitaRepository.UltimaVisitaPorMascota::getMascotaId,
                        CitaRepository.UltimaVisitaPorMascota::getFecha));

        return mascotaRepository.findActiveByCompanyId(companyId).stream()
                .map(m -> {
                    LocalDateTime ultima = ultimaVisitaPorMascota.get(m.getId());
                    LocalDate ultimaFecha = ultima != null ? ultima.toLocalDate() : null;
                    boolean inactiva = ultimaFecha == null || ultimaFecha.isBefore(umbral);
                    if (!inactiva) return null;
                    // Si nunca visitó, se cuentan los días desde que se registró la mascota
                    // (referencia más significativa que un valor arbitrario), y ese mismo
                    // número ordena la lista de más a menos urgente.
                    LocalDate referencia = ultimaFecha != null ? ultimaFecha
                            : (m.getCreatedAt() != null ? m.getCreatedAt().toLocalDate() : hoy);
                    long dias = ChronoUnit.DAYS.between(referencia, hoy);
                    return ReportesClinicosDTO.PacienteInactivo.builder()
                            .mascota(m.getNombreCompleto())
                            .apoderado(m.getApoderado() != null && m.getApoderado().getUser() != null
                                    ? m.getApoderado().getUser().getNombre() + " " + m.getApoderado().getUser().getApellido()
                                    : "Sin propietario")
                            .ultimaVisita(ultimaFecha != null ? ultimaFecha.toString() : "Nunca visitó")
                            .diasSinVisitar(dias)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingLong(ReportesClinicosDTO.PacienteInactivo::getDiasSinVisitar).reversed())
                .toList();
    }

    private ReportesClinicosDTO emptyReport(LocalDate desde, LocalDate hasta) {
        ReportesClinicosDTO.Resumen cero = ReportesClinicosDTO.Resumen.builder()
                .consultas(0).pacientesAtendidos(0).ingresos(BigDecimal.ZERO)
                .nuevosPacientes(0).tiempoPromedioAtencionMinutos(0)
                .porcentajeCitasCompletadas(0).noAsistieron(0).build();
        return ReportesClinicosDTO.builder()
                .fechaDesde(desde.toString()).fechaHasta(hasta.toString())
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
