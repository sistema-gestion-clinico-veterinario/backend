package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReporteComparativoEmpresas {

    private final CompanyRepository companyRepository;
    private final CitaRepository citaRepository;

    public ReportesComparativoEmpresasDTO obtener(LocalDate fechaDesde, LocalDate fechaHasta, EspecieMascota especie) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("Solo la administración de plataforma puede comparar empresas");
        }
        RangoDeReporte rango = RangoDeReporte.validado(fechaDesde, fechaHasta, AppClock.today());

        List<Company> activas = companyRepository.findAll().stream().filter(Company::isActivo).toList();
        Map<Integer, CitaRepository.ResumenPorEmpresa> resumenes = resumirPorEmpresa(activas, rango, especie);

        List<ReportesComparativoEmpresasDTO.EmpresaResumen> empresas = activas.stream()
                .map(company -> resumenDe(company, resumenes.get(company.getId())))
                .sorted(Comparator.comparingLong(ReportesComparativoEmpresasDTO.EmpresaResumen::getConsultas).reversed())
                .toList();

        return ReportesComparativoEmpresasDTO.builder()
                .fechaDesde(rango.desde().toString())
                .fechaHasta(rango.hasta().toString())
                .empresas(empresas)
                .build();
    }

    private Map<Integer, CitaRepository.ResumenPorEmpresa> resumirPorEmpresa(
            List<Company> empresas, RangoDeReporte rango, EspecieMascota especie) {
        if (empresas.isEmpty()) return Map.of();
        List<Integer> ids = empresas.stream().map(Company::getId).toList();
        return citaRepository.resumirPorEmpresa(ids, rango.inicio(), rango.finExclusivo(), especie).stream()
                .collect(Collectors.toMap(CitaRepository.ResumenPorEmpresa::getCompanyId, Function.identity()));
    }

    private ReportesComparativoEmpresasDTO.EmpresaResumen resumenDe(
            Company company, CitaRepository.ResumenPorEmpresa resumen) {
        if (resumen == null) {
            return construir(company, 0, 0, BigDecimal.ZERO, 0, 0, 0, 0);
        }
        return construir(company, resumen.getConsultas(), resumen.getPacientesAtendidos(), resumen.getIngresos(),
                resumen.getNuevosPacientes(), resumen.getCompletadas(), resumen.getCerradas(), resumen.getNoAsistieron());
    }

    private ReportesComparativoEmpresasDTO.EmpresaResumen construir(
            Company company, long consultas, long pacientesAtendidos, BigDecimal ingresos, long nuevosPacientes,
            long completadas, long cerradas, long noAsistieron) {
        return ReportesComparativoEmpresasDTO.EmpresaResumen.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .consultas(consultas)
                .pacientesAtendidos(pacientesAtendidos)
                .ingresos(ingresos.setScale(2, RoundingMode.HALF_UP))
                .nuevosPacientes(nuevosPacientes)
                .porcentajeCitasCompletadas(ReporteActividadClinica.porcentajeDeCompletadas(completadas, cerradas))
                .noAsistieron(noAsistieron)
                .build();
    }
}
