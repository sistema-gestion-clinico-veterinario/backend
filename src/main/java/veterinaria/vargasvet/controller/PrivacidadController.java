package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.CanalConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.dto.request.ConsentimientoFinalidadRequest;
import veterinaria.vargasvet.dto.request.PublicarAvisoPrivacidadRequest;
import veterinaria.vargasvet.dto.response.AvisoPrivacidadResponse;
import veterinaria.vargasvet.dto.response.AvisoPublicoResponse;
import veterinaria.vargasvet.dto.response.ConsentimientoEstadoResponse;
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
    public ResponseEntity<ApiResponse<AvisoPublicoResponse>> avisoPublico(@PathVariable String slug) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aviso de privacidad", avisoService.publico(slug)));
    }

    @GetMapping("/privacidad/aviso-vigente")
    public ResponseEntity<ApiResponse<AvisoPublicoResponse>> avisoVigente() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        AvisoPublicoResponse aviso = companyId == null ? null : avisoService.vigenteDeLaClinica(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, aviso == null ? "La clínica aún no publicó su aviso" : "Aviso de privacidad", aviso));
    }

    @GetMapping("/admin/privacidad/aviso/plantilla")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'LEER')")
    public ResponseEntity<ApiResponse<CamposAvisoPrivacidad>> plantilla() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Datos para el aviso",
                avisoService.plantilla(SecurityUtils.getCurrentCompanyId())));
    }

    @GetMapping("/admin/privacidad/aviso/historial")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'LEER')")
    public ResponseEntity<ApiResponse<List<AvisoPrivacidadResponse>>> historial() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Versiones del aviso",
                avisoService.historial(SecurityUtils.getCurrentCompanyId())));
    }

    @PostMapping("/admin/privacidad/aviso")
    @PreAuthorize("@accesoValidator.can('VISTA_COMPANY', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<AvisoPrivacidadResponse>> publicar(@Valid @RequestBody PublicarAvisoPrivacidadRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Aviso de privacidad publicado",
                avisoService.publicar(SecurityUtils.getCurrentCompanyId(), request)));
    }

    @GetMapping("/privacidad/mi-estado")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> miEstado() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de tu privacidad",
                consentimientoService.estado(SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentCompanyId())));
    }

    @PostMapping("/privacidad/enterado")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> enterado(HttpServletRequest http) {
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        consentimientoService.registrarEnterado(usuarioId, companyId, CanalConsentimiento.PORTAL, null,
                clientIpResolver.resolve(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Registramos que leíste el aviso",
                consentimientoService.estado(usuarioId, companyId)));
    }

    @PutMapping("/privacidad/finalidades/{finalidad}")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> decidir(@PathVariable FinalidadDatos finalidad,
                                                                             @Valid @RequestBody ConsentimientoFinalidadRequest request,
                                                                             HttpServletRequest http) {
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        consentimientoService.cambiarFinalidad(usuarioId, companyId, finalidad, request.isOtorgar(),
                CanalConsentimiento.PORTAL, null, request.getMotivo(), clientIpResolver.resolve(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, request.isOtorgar() ? "Registramos tu aceptación" : "Registramos que no aceptas",
                consentimientoService.estado(usuarioId, companyId)));
    }

    @GetMapping("/clients/guardians/{id}/privacidad")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'LEER')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> estadoDelCliente(@PathVariable Long id) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado de privacidad del cliente",
                consentimientoService.estado(usuarioId, companyId)));
    }

    @PostMapping("/clients/guardians/{id}/privacidad/enterado")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> informarAlCliente(@PathVariable Long id) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        if (!consentimientoService.registrarEnterado(usuarioId, companyId, CanalConsentimiento.PRESENCIAL,
                SecurityUtils.getCurrentUserId(), null, null)) {
            throw new IllegalStateException(ConsentimientoDatosService.MENSAJE_SIN_AVISO);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Registramos que el cliente fue informado",
                consentimientoService.estado(usuarioId, companyId)));
    }

    @PutMapping("/clients/guardians/{id}/privacidad/finalidades/{finalidad}")
    @PreAuthorize("@accesoValidator.can('VISTA_CLIENTES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<ConsentimientoEstadoResponse>> decidirPorElCliente(@PathVariable Long id,
                                                                                         @PathVariable FinalidadDatos finalidad,
                                                                                         @Valid @RequestBody ConsentimientoFinalidadRequest request) {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer usuarioId = consentimientoService.usuarioIdDelCliente(id, companyId);
        consentimientoService.cambiarFinalidad(usuarioId, companyId, finalidad, request.isOtorgar(),
                CanalConsentimiento.PRESENCIAL, SecurityUtils.getCurrentUserId(), request.getMotivo(), null, null);
        return ResponseEntity.ok(new ApiResponse<>(true, "Decisión registrada", consentimientoService.estado(usuarioId, companyId)));
    }
}
