package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.ConsentimientoFinalidadRequest;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.request.VistaPreviaAvisoRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.dto.response.AvisoPublicoResponse;
import veterinaria.vargasvet.dto.response.ConsentimientoEstadoResponse;
import veterinaria.vargasvet.dto.response.VistaPreviaAvisoResponse;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AvisoPrivacidadService;
import veterinaria.vargasvet.service.ConsentimientoDatosService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrivacidadController {

    private final AvisoPrivacidadService avisoService;
    private final ConsentimientoDatosService consentimientoService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/public/privacidad/{slug}")
    public ResponseEntity<ApiResponse<AvisoPublicoResponse>> avisoPublico(
            @PathVariable String slug,
            @RequestParam(defaultValue = "PROPIETARIOS_Y_AUTORIZADOS") AudienciaAvisoPrivacidad audiencia) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aviso de privacidad", avisoService.publico(slug, audiencia)));
    }

    @GetMapping("/public/privacidad/{slug}/version/{version}")
    public ResponseEntity<ApiResponse<AvisoPublicoResponse>> avisoPublicoVersion(
            @PathVariable String slug,
            @PathVariable Integer version,
            @RequestParam(defaultValue = "PROPIETARIOS_Y_AUTORIZADOS") AudienciaAvisoPrivacidad audiencia) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aviso de privacidad",
                avisoService.publico(slug, audiencia, version)));
    }

    @GetMapping("/privacidad/aviso-vigente")
    public ResponseEntity<ApiResponse<AvisoPublicoResponse>> avisoVigente(
            @RequestParam(required = false) AudienciaAvisoPrivacidad audiencia) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        AvisoPublicoResponse aviso = companyId == null ? null
                : avisoService.vigenteDeLaClinica(companyId, audiencia == null ? audienciaActual() : audiencia);
        return ResponseEntity.ok(new ApiResponse<>(true, aviso == null ? "La clínica aún no publicó su aviso" : "Aviso de privacidad", aviso));
    }

    @GetMapping("/admin/privacidad/aviso/plantilla")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<CamposAvisoPrivacidad>> plantilla(
            @RequestParam(defaultValue = "PROPIETARIOS_Y_AUTORIZADOS") AudienciaAvisoPrivacidad audiencia) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos para el aviso",
                avisoService.plantilla(SecurityUtils.getCurrentCompanyId(), audiencia)));
    }

    @GetMapping("/admin/privacidad/aviso/historial")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'LEER')")
    public ResponseEntity<ApiResponse<List<AvisoPrivacidadResponse>>> historial(
            @RequestParam(defaultValue = "PROPIETARIOS_Y_AUTORIZADOS") AudienciaAvisoPrivacidad audiencia) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Versiones del aviso",
                avisoService.historial(SecurityUtils.getCurrentCompanyId(), audiencia)));
    }

    @PostMapping("/admin/privacidad/aviso/vista-previa")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<VistaPreviaAvisoResponse>> vistaPrevia(@Valid @RequestBody VistaPreviaAvisoRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vista previa del aviso",
                avisoService.vistaPrevia(SecurityUtils.getCurrentCompanyId(), request)));
    }

    @PostMapping("/admin/privacidad/aviso")
    @PreAuthorize("@accesoValidator.hasPurpose('COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<AvisoPrivacidadResponse>> publicar(@Valid @RequestBody PublicarAvisoPrivacidadRequest request,
                                                                         HttpServletRequest http) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aviso de privacidad publicado",
                avisoService.publicar(SecurityUtils.getCurrentCompanyId(), request,
                        clientIpResolver.resolve(http), http.getHeader("User-Agent"))));
    }

    @GetMapping("/privacidad/mi-estado")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> miEstado() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de tu privacidad",
                consentimientoService.estado(SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentCompanyId(), audienciaActual())));
    }

    @PostMapping("/privacidad/enterado")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> enterado(HttpServletRequest http) {
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        consentimientoService.registrarEnterado(usuarioId, companyId, audienciaActual(), CanalConsentimiento.PORTAL, null,
                clientIpResolver.resolve(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Registramos que leíste el aviso",
                consentimientoService.estado(usuarioId, companyId, audienciaActual())));
    }

    @PutMapping("/privacidad/finalidades/{finalidad}")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> decidir(@PathVariable FinalidadDatos finalidad,
                                                                             @Valid @RequestBody ConsentimientoFinalidadRequest request,
                                                                             HttpServletRequest http) {
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        consentimientoService.cambiarFinalidad(usuarioId, companyId, audienciaActual(), finalidad, request.isOtorgar(),
                CanalConsentimiento.PORTAL, usuarioId, request.getMotivo(), clientIpResolver.resolve(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, request.isOtorgar() ? "Autorización registrada" : "Revocación registrada",
                consentimientoService.estado(usuarioId, companyId, audienciaActual())));
    }

    @GetMapping("/clients/guardians/{id}/privacidad")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'LEER')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> estadoDelCliente(@PathVariable Long id) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de privacidad del cliente",
                consentimientoService.estado(usuarioId, companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)));
    }

    @PostMapping("/clients/guardians/{id}/privacidad/enterado")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> informarAlCliente(@PathVariable Long id) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        if (!consentimientoService.registrarEnterado(usuarioId, companyId,
                AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS, CanalConsentimiento.PRESENCIAL,
                SecurityUtils.getCurrentUserId(), null, null)) {
            throw new IllegalStateException(ConsentimientoDatosService.MENSAJE_SIN_AVISO);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Registramos que el cliente fue informado",
                consentimientoService.estado(usuarioId, companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)));
    }

    @PutMapping("/clients/guardians/{id}/privacidad/finalidades/{finalidad}")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> decidirPorElCliente(@PathVariable Long id,
                                                                                         @PathVariable FinalidadDatos finalidad,
                                                                                         @Valid @RequestBody ConsentimientoFinalidadRequest request) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        consentimientoService.cambiarFinalidad(usuarioId, companyId,
                AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS, finalidad, request.isOtorgar(),
                CanalConsentimiento.PRESENCIAL, SecurityUtils.getCurrentUserId(), request.getMotivo(), null, null);
        return ResponseEntity.ok(new ApiResponse<>(true, "Decisión registrada",
                consentimientoService.estado(usuarioId, companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS)));
    }

    private AudienciaAvisoPrivacidad audienciaActual() {
        return SecurityUtils.getCurrentRolePurpose() == RolePurpose.CLIENT_PORTAL
                ? AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS
                : AudienciaAvisoPrivacidad.TRABAJADORES_Y_USUARIOS;
    }
}
