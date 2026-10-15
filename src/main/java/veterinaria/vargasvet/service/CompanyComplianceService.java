package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.CompanyCompliancePolicy;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;
import veterinaria.vargasvet.dto.request.ConfigurarCumplimientoClinicaRequest;
import veterinaria.vargasvet.dto.response.ConfiguracionCumplimientoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyCompliancePolicyRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyComplianceService {
    private final CompanyCompliancePolicyRepository repository;
    private final CompanyRepository companyRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ConfiguracionCumplimientoResponse resolver(Integer companyId, ControlCumplimientoClinica control) {
        if (companyId == null) return porDefecto(control);
        return repository.vigente(companyId, control, AppClock.now()).map(this::response).orElseGet(() -> porDefecto(control));
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionCumplimientoResponse> actuales(Integer companyId) {
        companyRepository.findById(companyId).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return Arrays.stream(ControlCumplimientoClinica.values()).map(c -> resolver(companyId, c)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionCumplimientoResponse> historial(Integer companyId) {
        return repository.findByCompanyIdOrderByControlAscVersionDesc(companyId).stream().map(this::response).toList();
    }

    @Transactional
    public ConfiguracionCumplimientoResponse configurar(Integer companyId, ControlCumplimientoClinica control,
                                                         ConfigurarCumplimientoClinicaRequest request) {
        validar(control, request);
        validarDependencias(companyId, control, request);
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No se identificó al administrador de plataforma");
        LocalDateTime ahora = AppClock.now();
        CompanyCompliancePolicy policy = new CompanyCompliancePolicy();
        policy.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        policy.setControl(control);
        policy.setVersion(repository.ultimaVersion(companyId, control) + 1);
        policy.setHabilitado(request.habilitado());
        policy.setModo(request.modo());
        policy.setBloqueante(request.bloqueante());
        policy.setJustificacion(request.justificacion().trim());
        policy.setReferenciaContractual(limpiar(request.referenciaContractual()));
        policy.setConfirmoRevisionLegal(true);
        policy.setVigenteDesde(request.vigenteDesde() == null ? ahora : request.vigenteDesde());
        policy.setCreadaAt(ahora);
        policy.setCreadaPor(usuarioRepository.getReferenceById(userId));
        repository.save(policy);
        auditLogService.log(companyId, "CONFIGURAR_CONTROL_CUMPLIMIENTO", "Administración de plataforma",
                control + " v" + policy.getVersion() + ": habilitado=" + policy.isHabilitado()
                        + ", modo=" + policy.getModo() + ", bloqueante=" + policy.isBloqueante()
                        + ", vigencia=" + policy.getVigenteDesde());
        return response(policy);
    }

    public boolean gestionadoPorSoftvet(Integer companyId, ControlCumplimientoClinica control) {
        return resolver(companyId, control).gestionadoPorSoftvet();
    }

    private void validar(ControlCumplimientoClinica control, ConfigurarCumplimientoClinicaRequest r) {
        if (!r.confirmoRevisionLegal()) throw new IllegalArgumentException("Debe confirmar la revisión legal de esta configuración");
        if (!r.habilitado() && (r.modo() != ModoGestionCumplimiento.NO_APLICA || r.bloqueante())) {
            throw new IllegalArgumentException("Un control deshabilitado debe usar NO_APLICA y no puede ser bloqueante");
        }
        if (r.habilitado() && r.modo() == ModoGestionCumplimiento.NO_APLICA) {
            throw new IllegalArgumentException("Un control habilitado debe gestionarse en SoftVet o externamente");
        }
        if (r.modo() == ModoGestionCumplimiento.EXTERNO && r.bloqueante()) {
            throw new IllegalArgumentException("SoftVet no puede bloquear por una evidencia que se gestiona externamente");
        }
        if (control == ControlCumplimientoClinica.RECORDATORIOS_CORREO && r.habilitado()
                && r.modo() != ModoGestionCumplimiento.SOFTVET) {
            throw new IllegalArgumentException("Los recordatorios automáticos requieren gestionar la autorización en SoftVet");
        }
    }

    private void validarDependencias(Integer companyId, ControlCumplimientoClinica control,
                                     ConfigurarCumplimientoClinicaRequest request) {
        boolean consentimientoInterno = request.habilitado()
                && request.modo() == ModoGestionCumplimiento.SOFTVET
                && (control == ControlCumplimientoClinica.USO_IA
                    || control == ControlCumplimientoClinica.RECORDATORIOS_CORREO);
        if (consentimientoInterno
                && !gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.AVISO_PRIVACIDAD)) {
            throw new IllegalArgumentException(
                    "Para registrar esta autorización en SoftVet, primero habilita el aviso de privacidad gestionado en SoftVet");
        }

        boolean desactivaAvisoInterno = control == ControlCumplimientoClinica.AVISO_PRIVACIDAD
                && (!request.habilitado() || request.modo() != ModoGestionCumplimiento.SOFTVET);
        if (desactivaAvisoInterno
                && (gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.USO_IA)
                    || gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.RECORDATORIOS_CORREO))) {
            throw new IllegalArgumentException(
                    "Primero deshabilita o cambia la gestión de IA y recordatorios; ambos dependen del aviso de privacidad de SoftVet");
        }
    }

    private ConfiguracionCumplimientoResponse porDefecto(ControlCumplimientoClinica c) {
        return new ConfiguracionCumplimientoResponse(c, 0, c.isHabilitadoPorDefecto(), c.getModoPorDefecto(),
                c.isBloqueantePorDefecto(), "Configuración segura predeterminada del sistema", null,
                null, null, "Sistema", false);
    }

    private ConfiguracionCumplimientoResponse response(CompanyCompliancePolicy p) {
        Usuario u = p.getCreadaPor();
        String nombre = ((u.getNombre() == null ? "" : u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido())).trim();
        return new ConfiguracionCumplimientoResponse(p.getControl(), p.getVersion(), p.isHabilitado(), p.getModo(),
                p.isBloqueante(), p.getJustificacion(), p.getReferenciaContractual(), p.getVigenteDesde(), p.getCreadaAt(),
                nombre.isBlank() ? u.getEmail() : nombre, true);
    }

    private String limpiar(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
