package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.enums.TipoRelacionMascota;
import veterinaria.vargasvet.dto.request.MascotaRelacionRequest;
import veterinaria.vargasvet.dto.response.MascotaRelacionResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.MascotaRelacionService;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaRelacionServiceImpl implements MascotaRelacionService {

    private final MascotaPersonaRelacionRepository relacionRepository;
    private final MascotaRepository mascotaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<MascotaRelacionResponse> listar(String mascotaUuid) {
        Mascota mascota = obtenerMascotaAccesible(mascotaUuid);
        Integer companyId = companyIdDe(mascota);
        return relacionRepository.findByMascotaUuidAndCompanyId(mascotaUuid, companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MascotaRelacionResponse crear(String mascotaUuid, MascotaRelacionRequest request) {
        Mascota mascota = obtenerMascotaAccesible(mascotaUuid);
        Integer companyId = companyIdDe(mascota);
        validarTipoGestionable(request.getTipoRelacion());
        Apoderado apoderado = obtenerApoderadoAccesible(request.getApoderadoId(), companyId);

        var relacionExistente = relacionRepository
                .findByMascotaIdAndApoderadoId(mascota.getId(), apoderado.getId());
        if (relacionExistente.isPresent() && estaVigente(relacionExistente.get())) {
            throw new IllegalArgumentException("La persona ya tiene una relación activa con esta mascota");
        }
        MascotaPersonaRelacion relacion = relacionExistente.orElseGet(MascotaPersonaRelacion::new);

        relacion.setMascota(mascota);
        relacion.setApoderado(apoderado);
        relacion.setCompany(apoderado.getCompany());
        relacion.setFechaInicio(AppClock.today());
        relacion.setFechaFin(request.getFechaFin());
        relacion.setObservaciones(normalizar(request.getObservaciones()));
        relacion.setActivo(true);
        relacion.setRevokedAt(null);
        relacion.setRevokedBy(null);
        relacion.setCreatedBy(valorAuditoria());
        relacion.setUpdatedBy(valorAuditoria());
        aplicarAutorizaciones(relacion, request);

        MascotaPersonaRelacion guardada = relacionRepository.save(relacion);
        auditLogService.log(companyId, "VINCULAR_PERSONA_MASCOTA", "Mascotas",
                "Se vinculó a " + nombre(apoderado) + " como " + etiqueta(guardada.getTipoRelacion())
                        + " de la mascota " + mascota.getNombreCompleto());
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public MascotaRelacionResponse actualizar(String mascotaUuid, String relacionUuid,
                                                MascotaRelacionRequest request) {
        Mascota mascota = obtenerMascotaAccesible(mascotaUuid);
        Integer companyId = companyIdDe(mascota);
        MascotaPersonaRelacion relacion = obtenerRelacionAccesible(relacionUuid, companyId, mascotaUuid);
        if (relacion.getTipoRelacion() == TipoRelacionMascota.PROPIETARIO_PRINCIPAL) {
            throw new IllegalArgumentException("El propietario principal se modifica mediante una transferencia de titularidad");
        }
        if (!estaVigente(relacion)) {
            throw new IllegalStateException("No se puede modificar una relación revocada o vencida");
        }
        if (!relacion.getApoderado().getId().equals(request.getApoderadoId())) {
            throw new IllegalArgumentException("No se puede cambiar la persona de una relación existente");
        }
        validarTipoGestionable(request.getTipoRelacion());

        relacion.setFechaFin(request.getFechaFin());
        relacion.setObservaciones(normalizar(request.getObservaciones()));
        relacion.setUpdatedBy(valorAuditoria());
        aplicarAutorizaciones(relacion, request);
        MascotaPersonaRelacion guardada = relacionRepository.save(relacion);
        auditLogService.log(companyId, "ACTUALIZAR_RELACION_MASCOTA", "Mascotas",
                "Se actualizaron las autorizaciones de " + nombre(relacion.getApoderado())
                        + " para la mascota " + mascota.getNombreCompleto());
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public void revocar(String mascotaUuid, String relacionUuid) {
        Mascota mascota = obtenerMascotaAccesible(mascotaUuid);
        Integer companyId = companyIdDe(mascota);
        MascotaPersonaRelacion relacion = obtenerRelacionAccesible(relacionUuid, companyId, mascotaUuid);
        if (relacion.getTipoRelacion() == TipoRelacionMascota.PROPIETARIO_PRINCIPAL) {
            throw new IllegalArgumentException("No se puede revocar al propietario principal; primero debe transferirse la titularidad");
        }
        if (!estaVigente(relacion)) return;

        relacion.setActivo(false);
        relacion.setFechaFin(AppClock.today());
        relacion.setRevokedAt(AppClock.now());
        relacion.setRevokedBy(valorAuditoria());
        relacion.setUpdatedBy(valorAuditoria());
        relacionRepository.save(relacion);
        auditLogService.log(companyId, "REVOCAR_RELACION_MASCOTA", "Mascotas",
                "Se revocó la relación de " + nombre(relacion.getApoderado())
                        + " con la mascota " + mascota.getNombreCompleto());
    }

    @Override
    @Transactional
    public void asegurarPropietarioPrincipal(Long mascotaId) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        Apoderado principal = mascota.getApoderado();
        if (principal == null || principal.getCompany() == null) {
            throw new IllegalStateException("La mascota no tiene un propietario principal válido");
        }

        List<MascotaPersonaRelacion> relaciones = relacionRepository
                .findByMascotaUuidAndCompanyId(mascota.getUuid(), principal.getCompany().getId());
        String usuario = valorAuditoria();
        for (MascotaPersonaRelacion existente : relaciones) {
            if (existente.getTipoRelacion() == TipoRelacionMascota.PROPIETARIO_PRINCIPAL
                    && !existente.getApoderado().getId().equals(principal.getId())
                    && Boolean.TRUE.equals(existente.getActivo())) {
                existente.setActivo(false);
                existente.setFechaFin(AppClock.today());
                existente.setRevokedAt(AppClock.now());
                existente.setRevokedBy(usuario);
                existente.setUpdatedBy(usuario);
                relacionRepository.save(existente);
            }
        }

        MascotaPersonaRelacion relacion = relacionRepository
                .findByMascotaIdAndApoderadoId(mascota.getId(), principal.getId())
                .orElseGet(MascotaPersonaRelacion::new);
        relacion.setMascota(mascota);
        relacion.setApoderado(principal);
        relacion.setCompany(principal.getCompany());
        relacion.setTipoRelacion(TipoRelacionMascota.PROPIETARIO_PRINCIPAL);
        relacion.setPuedeRecibirInformacion(true);
        relacion.setPuedeAutorizarAtencion(true);
        relacion.setPuedeRealizarPagos(true);
        relacion.setFechaInicio(AppClock.today());
        relacion.setFechaFin(null);
        relacion.setActivo(true);
        relacion.setRevokedAt(null);
        relacion.setRevokedBy(null);
        if (relacion.getCreatedBy() == null) relacion.setCreatedBy(usuario);
        relacion.setUpdatedBy(usuario);
        relacionRepository.save(relacion);
    }

    private void aplicarAutorizaciones(MascotaPersonaRelacion relacion, MascotaRelacionRequest request) {
        relacion.setTipoRelacion(request.getTipoRelacion());
        switch (request.getTipoRelacion()) {
            case COPROPIETARIO -> {
                relacion.setPuedeRecibirInformacion(true);
                relacion.setPuedeAutorizarAtencion(true);
                relacion.setPuedeRealizarPagos(true);
            }
            case RESPONSABLE_PAGO -> {
                relacion.setPuedeRecibirInformacion(false);
                relacion.setPuedeAutorizarAtencion(false);
                relacion.setPuedeRealizarPagos(true);
            }
            case REPRESENTANTE_AUTORIZADO -> {
                boolean informacion = Boolean.TRUE.equals(request.getPuedeRecibirInformacion());
                boolean atencion = Boolean.TRUE.equals(request.getPuedeAutorizarAtencion());
                boolean pagos = Boolean.TRUE.equals(request.getPuedeRealizarPagos());
                if (!informacion && !atencion && !pagos) {
                    throw new IllegalArgumentException("Seleccione al menos una autorización para el representante");
                }
                relacion.setPuedeRecibirInformacion(informacion);
                relacion.setPuedeAutorizarAtencion(atencion);
                relacion.setPuedeRealizarPagos(pagos);
            }
            case PROPIETARIO_PRINCIPAL -> throw new IllegalArgumentException(
                    "El propietario principal se administra mediante la titularidad de la mascota");
        }
    }

    private void validarTipoGestionable(TipoRelacionMascota tipo) {
        if (tipo == TipoRelacionMascota.PROPIETARIO_PRINCIPAL) {
            throw new IllegalArgumentException("No se puede agregar otro propietario principal desde esta opción");
        }
    }

    private Mascota obtenerMascotaAccesible(String uuid) {
        if (uuid == null || uuid.isBlank()) throw new ResourceNotFoundException("Mascota no encontrada");
        if (SecurityUtils.isSuperAdmin()) {
            return mascotaRepository.findByUuid(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        }
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) throw new AccessDeniedException("El usuario no tiene una empresa asignada");
        return mascotaRepository.findByUuidAndCompanyId(uuid, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
    }

    private Apoderado obtenerApoderadoAccesible(Long id, Integer companyId) {
        Apoderado apoderado = apoderadoRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada en esta empresa"));
        if (!Boolean.TRUE.equals(apoderado.getEstado())) {
            throw new IllegalArgumentException("No se puede vincular una persona inactiva");
        }
        return apoderado;
    }

    private MascotaPersonaRelacion obtenerRelacionAccesible(String uuid, Integer companyId, String mascotaUuid) {
        MascotaPersonaRelacion relacion = relacionRepository.findByUuidAndCompanyId(uuid, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Relación no encontrada"));
        if (!relacion.getMascota().getUuid().equals(mascotaUuid)) {
            throw new ResourceNotFoundException("Relación no encontrada");
        }
        return relacion;
    }

    private Integer companyIdDe(Mascota mascota) {
        if (mascota.getApoderado() == null || mascota.getApoderado().getCompany() == null) {
            throw new IllegalStateException("La mascota no pertenece a una empresa válida");
        }
        return mascota.getApoderado().getCompany().getId();
    }

    private MascotaRelacionResponse toResponse(MascotaPersonaRelacion relacion) {
        MascotaRelacionResponse response = new MascotaRelacionResponse();
        response.setUuid(relacion.getUuid());
        response.setMascotaUuid(relacion.getMascota().getUuid());
        response.setMascotaNombre(relacion.getMascota().getNombreCompleto());
        response.setApoderadoId(relacion.getApoderado().getId());
        response.setPersonaNombre(nombre(relacion.getApoderado()));
        response.setNumeroDocumento(relacion.getApoderado().getNumeroDocumento());
        response.setTipoRelacion(relacion.getTipoRelacion());
        response.setPuedeRecibirInformacion(relacion.getPuedeRecibirInformacion());
        response.setPuedeAutorizarAtencion(relacion.getPuedeAutorizarAtencion());
        response.setPuedeRealizarPagos(relacion.getPuedeRealizarPagos());
        response.setFechaInicio(relacion.getFechaInicio());
        response.setFechaFin(relacion.getFechaFin());
        response.setObservaciones(relacion.getObservaciones());
        response.setActivo(estaVigente(relacion));
        response.setCreatedAt(relacion.getCreatedAt());
        response.setCreatedBy(relacion.getCreatedBy());
        response.setUpdatedAt(relacion.getUpdatedAt());
        response.setUpdatedBy(relacion.getUpdatedBy());
        return response;
    }

    private String nombre(Apoderado apoderado) {
        if (apoderado.getUser() == null) return "Persona registrada";
        return (apoderado.getUser().getNombre() + " " + apoderado.getUser().getApellido()).trim();
    }

    private String etiqueta(TipoRelacionMascota tipo) {
        return tipo.name().toLowerCase().replace('_', ' ');
    }

    private String normalizar(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String valorAuditoria() {
        String email = SecurityUtils.getCurrentUserEmail();
        return email == null || email.isBlank() ? "SYSTEM" : email;
    }

    private boolean estaVigente(MascotaPersonaRelacion relacion) {
        return Boolean.TRUE.equals(relacion.getActivo())
                && (relacion.getFechaFin() == null || !relacion.getFechaFin().isBefore(AppClock.today()));
    }
}
