package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.MascotaRelacionService;
import veterinaria.vargasvet.service.PetLinkNotifier;
import veterinaria.vargasvet.service.PetOwnershipService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MascotaRelacionServiceImpl implements MascotaRelacionService {

    private static final int LOTE_VENCIDAS = 200;

    private final MascotaPersonaRelacionRepository relacionRepository;
    private final MascotaRepository mascotaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AuditLogService auditLogService;
    private final PetOwnershipService petOwnershipService;
    private final PetLinkNotifier petLinkNotifier;
    private final ApoderadoService apoderadoService;

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
        if (Objects.equals(mascota.getApoderado().getId(), apoderado.getId())) {
            throw new IllegalArgumentException("La persona ya es la propietaria principal de esta mascota");
        }

        LocalDate hoy = AppClock.today();
        LocalDate inicio = request.getFechaInicio() != null ? request.getFechaInicio() : hoy;
        if (inicio.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy");
        }
        if (request.getFechaFin() != null && request.getFechaFin().isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        for (MascotaPersonaRelacion abierta : relacionRepository
                .findAllByMascotaIdAndApoderadoIdAndActivoTrue(mascota.getId(), apoderado.getId())) {
            if (!estaVencida(abierta)) {
                throw new IllegalArgumentException("La persona ya tiene una relación activa con esta mascota");
            }
            cerrarPorVencimiento(abierta, companyId);
        }

        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(apoderado);
        relacion.setCompany(apoderado.getCompany());
        relacion.setFechaInicio(inicio);
        relacion.setFechaFin(request.getFechaFin());
        relacion.setObservaciones(normalizar(request.getObservaciones()));
        relacion.setActivo(true);
        relacion.setCreatedBy(valorAuditoria());
        relacion.setUpdatedBy(valorAuditoria());
        aplicarAutorizaciones(relacion, request);

        MascotaPersonaRelacion guardada = relacionRepository.save(relacion);
        String detalle = describir(guardada);
        auditLogService.log(companyId, "VINCULAR_PERSONA_MASCOTA", "Mascotas",
                "Se vinculó a " + nombre(apoderado) + " como " + etiqueta(guardada.getTipoRelacion())
                        + " de la mascota " + mascota.getNombreCompleto() + ": " + detalle);
        petLinkNotifier.avisarAlPrincipal(mascota, apoderado, "vinculó", detalle);
        if (Boolean.TRUE.equals(request.getDarAccesoPortal())) {
            apoderadoService.invitarAcceso(apoderado.getId());
        }
        MascotaRelacionResponse respuesta = toResponse(guardada);
        respuesta.setAvisoMascota(petOwnershipService.reevaluate(mascota));
        return respuesta;
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
        if (!estaVigenteOPorEmpezar(relacion)) {
            throw new IllegalStateException("No se puede modificar una relación revocada o vencida");
        }
        if (!relacion.getApoderado().getId().equals(request.getApoderadoId())) {
            throw new IllegalArgumentException("No se puede cambiar la persona de una relación existente");
        }
        validarTipoGestionable(request.getTipoRelacion());

        LocalDate hoy = AppClock.today();
        boolean todaviaNoEmpieza = relacion.getFechaInicio().isAfter(hoy);
        LocalDate inicio = todaviaNoEmpieza && request.getFechaInicio() != null ? request.getFechaInicio() : relacion.getFechaInicio();
        if (todaviaNoEmpieza && inicio.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy");
        }
        if (request.getFechaFin() != null && request.getFechaFin().isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        String antes = describir(relacion);
        TipoRelacionMascota tipoAnterior = relacion.getTipoRelacion();
        relacion.setFechaInicio(inicio);
        relacion.setFechaFin(request.getFechaFin());
        relacion.setObservaciones(normalizar(request.getObservaciones()));
        relacion.setUpdatedBy(valorAuditoria());
        aplicarAutorizaciones(relacion, request);
        String despues = describir(relacion);
        MascotaPersonaRelacion guardada = relacionRepository.save(relacion);

        if (!antes.equals(despues) || tipoAnterior != guardada.getTipoRelacion()) {
            String cambio = (tipoAnterior != guardada.getTipoRelacion()
                    ? "pasó de " + etiqueta(tipoAnterior) + " a " + etiqueta(guardada.getTipoRelacion()) + ". " : "")
                    + "Antes: " + antes + ". Ahora: " + despues;
            auditLogService.log(companyId, "ACTUALIZAR_RELACION_MASCOTA", "Mascotas",
                    "Se actualizó el vínculo de " + nombre(relacion.getApoderado()) + " con la mascota "
                            + mascota.getNombreCompleto() + ": " + cambio);
            petLinkNotifier.avisarAlPrincipal(mascota, relacion.getApoderado(), "modificó", cambio);
        }
        MascotaRelacionResponse respuesta = toResponse(guardada);
        respuesta.setAvisoMascota(petOwnershipService.reevaluate(mascota));
        return respuesta;
    }

    @Override
    @Transactional
    public String revocar(String mascotaUuid, String relacionUuid) {
        Mascota mascota = obtenerMascotaAccesible(mascotaUuid);
        Integer companyId = companyIdDe(mascota);
        MascotaPersonaRelacion relacion = obtenerRelacionAccesible(relacionUuid, companyId, mascotaUuid);
        if (relacion.getTipoRelacion() == TipoRelacionMascota.PROPIETARIO_PRINCIPAL) {
            throw new IllegalArgumentException("No se puede revocar al propietario principal; primero debe transferirse la titularidad");
        }
        if (!Boolean.TRUE.equals(relacion.getActivo())) return null;

        String detalle = describir(relacion);
        relacion.setActivo(false);
        if (relacion.getFechaFin() == null || relacion.getFechaFin().isAfter(AppClock.today())) {
            relacion.setFechaFin(AppClock.today().isBefore(relacion.getFechaInicio()) ? relacion.getFechaInicio() : AppClock.today());
        }
        relacion.setRevokedAt(AppClock.now());
        relacion.setRevokedBy(valorAuditoria());
        relacion.setUpdatedBy(valorAuditoria());
        relacionRepository.save(relacion);
        auditLogService.log(companyId, "REVOCAR_RELACION_MASCOTA", "Mascotas",
                "Se revocó el vínculo de " + nombre(relacion.getApoderado())
                        + " con la mascota " + mascota.getNombreCompleto() + ". Tenía: " + detalle);
        petLinkNotifier.avisarAlPrincipal(mascota, relacion.getApoderado(), "revocó", "Tenía: " + detalle);
        return petOwnershipService.reevaluate(mascota);
    }

    @Override
    @Transactional
    public int cerrarVencidas() {
        List<MascotaPersonaRelacion> vencidas = relacionRepository.findVencidasSinCerrar(
                AppClock.today(), PageRequest.of(0, LOTE_VENCIDAS));
        for (MascotaPersonaRelacion relacion : vencidas) {
            Integer companyId = relacion.getCompany() != null ? relacion.getCompany().getId() : null;
            cerrarPorVencimiento(relacion, companyId);
            petOwnershipService.reevaluate(relacion.getMascota());
        }
        return vencidas.size();
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
                existente.setFechaFin(AppClock.today().isBefore(existente.getFechaInicio()) ? existente.getFechaInicio() : AppClock.today());
                existente.setRevokedAt(AppClock.now());
                existente.setRevokedBy(usuario);
                existente.setUpdatedBy(usuario);
                relacionRepository.save(existente);
            }
        }

        if (relacionRepository.findFirstByMascotaIdAndApoderadoIdAndTipoRelacionAndActivoTrue(
                mascota.getId(), principal.getId(), TipoRelacionMascota.PROPIETARIO_PRINCIPAL).isPresent()) {
            return;
        }
        for (MascotaPersonaRelacion otra : relacionRepository
                .findAllByMascotaIdAndApoderadoIdAndActivoTrue(mascota.getId(), principal.getId())) {
            otra.setActivo(false);
            otra.setFechaFin(AppClock.today().isBefore(otra.getFechaInicio()) ? otra.getFechaInicio() : AppClock.today());
            otra.setRevokedAt(AppClock.now());
            otra.setRevokedBy(usuario);
            otra.setUpdatedBy(usuario);
            relacionRepository.save(otra);
        }
        MascotaPersonaRelacion relacion = new MascotaPersonaRelacion();
        relacion.setMascota(mascota);
        relacion.setApoderado(principal);
        relacion.setCompany(principal.getCompany());
        relacion.setTipoRelacion(TipoRelacionMascota.PROPIETARIO_PRINCIPAL);
        relacion.setPuedeRecibirInformacion(true);
        relacion.setPuedeAutorizarAtencion(true);
        relacion.setPuedeRealizarPagos(true);
        relacion.setFechaInicio(AppClock.today());
        relacion.setActivo(true);
        relacion.setCreatedBy(usuario);
        relacion.setUpdatedBy(usuario);
        relacionRepository.save(relacion);
    }

    private void cerrarPorVencimiento(MascotaPersonaRelacion relacion, Integer companyId) {
        relacion.setActivo(false);
        relacion.setRevokedAt(AppClock.now());
        relacion.setRevokedBy("SISTEMA (vigencia vencida)");
        relacion.setUpdatedBy("SISTEMA");
        relacionRepository.save(relacion);
        auditLogService.log(companyId, "VENCER_RELACION_MASCOTA", "Mascotas",
                "Venció el vínculo de " + nombre(relacion.getApoderado()) + " con la mascota "
                        + relacion.getMascota().getNombreCompleto() + ". Tenía: " + describir(relacion));
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
        response.setPorEmpezar(Boolean.TRUE.equals(relacion.getActivo()) && relacion.getFechaInicio().isAfter(AppClock.today())
                && !estaVencida(relacion));
        response.setCuentaActivada(relacion.getApoderado().getUser() != null
                && relacion.getApoderado().getUser().isActivo() && relacion.getApoderado().getUser().isEmailVerified());
        response.setCreatedAt(relacion.getCreatedAt());
        response.setCreatedBy(relacion.getCreatedBy());
        response.setUpdatedAt(relacion.getUpdatedAt());
        response.setUpdatedBy(relacion.getUpdatedBy());
        return response;
    }

    private String describir(MascotaPersonaRelacion relacion) {
        List<String> permisos = new ArrayList<>();
        permisos.add("recibir información: " + sino(relacion.getPuedeRecibirInformacion()));
        permisos.add("autorizar atención: " + sino(relacion.getPuedeAutorizarAtencion()));
        permisos.add("realizar pagos: " + sino(relacion.getPuedeRealizarPagos()));
        return String.join(", ", permisos) + "; vigente desde " + relacion.getFechaInicio()
                + (relacion.getFechaFin() == null ? " sin fecha de fin" : " hasta " + relacion.getFechaFin());
    }

    private String sino(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "sí" : "no";
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

    private boolean estaVencida(MascotaPersonaRelacion relacion) {
        return relacion.getFechaFin() != null && relacion.getFechaFin().isBefore(AppClock.today());
    }

    private boolean estaVigenteOPorEmpezar(MascotaPersonaRelacion relacion) {
        return Boolean.TRUE.equals(relacion.getActivo()) && !estaVencida(relacion);
    }

    private boolean estaVigente(MascotaPersonaRelacion relacion) {
        return estaVigenteOPorEmpezar(relacion) && !relacion.getFechaInicio().isAfter(AppClock.today());
    }
}
