package veterinaria.vargasvet.service.reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReporteComparativoEmpresas {

    private final CompanyRepository companyRepository;
    private final CitaRepository citaRepository;
    private final ReporteActividadClinica actividad;

    public ReportesComparativoEmpresasDTO obtener(LocalDate fechaDesde, LocalDate fechaHasta, EspecieMascota especie) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new IllegalArgumentException("Solo la administración de plataforma puede comparar empresas");
        }
        RangoDeReporte rango = RangoDeReporte.validado(fechaDesde, fechaHasta, AppClock.today());

        List<ReportesComparativoEmpresasDTO.EmpresaResumen> empresas = companyRepository.findAll().stream()
                .filter(Company::isActivo)
                .map(company -> resumenDe(company, rango, especie))
                .sorted(Comparator.comparingLong(ReportesComparativoEmpresasDTO.EmpresaResumen::getConsultas).reversed())
                .toList();

        return ReportesComparativoEmpresasDTO.builder()
                .fechaDesde(rango.desde().toString())
                .fechaHasta(rango.hasta().toString())
                .empresas(empresas)
                .build();
    }

    private ReportesComparativoEmpresasDTO.EmpresaResumen resumenDe(Company company, RangoDeReporte rango,
                                                                    EspecieMascota especie) {
        List<Cita> citas = citaRepository.findForClinicalReport(
                company.getId(), rango.inicio(), rango.finExclusivo(), null, especie);
        ReportesClinicosDTO.Resumen resumen = actividad.resumen(citas, rango, true);
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
    }
}
