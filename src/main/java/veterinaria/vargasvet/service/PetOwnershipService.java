package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;
import veterinaria.vargasvet.domain.enums.MotivoBajaMascota;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.response.ApoderadoEstadoResponse;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.MascotaPersonaRelacionRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una mascota está operable mientras alguna persona ACTIVA de su empresa pueda autorizar su atención
 * (la propietaria principal o una copropietaria vigente). Cuando una persona se suspende, se da de
 * baja o se reactiva, las mascotas se pausan o restauran según eso, sin tocar las que el personal
 * dio de baja por otra causa (por ejemplo, un fallecimiento).
 *
 * <p>Todo se resuelve dentro de la empresa de la persona: una misma persona puede ser cliente de
 * varias clínicas, y lo que ocurra en una nunca afecta a las mascotas de otra.
 */
@Service
@RequiredArgsConstructor
public class PetOwnershipService {

    private final MascotaRepository mascotaRepository;
    private final MascotaPersonaRelacionRepository relacionRepository;
    private final CitaRepository citaRepository;
    private final AuditLogService auditLogService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /** Bloquea a las personas que autorizan la atención de la mascota y vuelve a leer su estado y el de la mascota:
     * agendar y dar de baja a su dueño no pueden cruzarse. Se bloquean en orden de id para no cruzarse entre sí. */
    public void lockAuthorizers(Mascota mascota) {
        Apoderado principal = mascota.getApoderado();
        if (principal == null) return;
        java.util.TreeMap<Long, Apoderado> toLock = new java.util.TreeMap<>();
        toLock.put(principal.getId(), principal);
        Integer companyId = companyIdOf(principal);
        if (companyId != null) {
            relacionRepository.findAuthorizers(mascota.getId(), companyId, AppClock.today())
                    .forEach(person -> toLock.putIfAbsent(person.getId(), person));
        }
        toLock.values().forEach(person -> {
            entityManager.lock(person, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(person);
        });
        entityManager.refresh(mascota);
    }

    /** Bloquea la mascota y vuelve a leerla: dos operaciones sobre la misma mascota (restaurarla al reactivar a su dueño,
     * registrar su fallecimiento) se atienden de una en una y la segunda ve el resultado de la primera. */
    public void lockPet(Mascota mascota) {
        entityManager.lock(mascota, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(mascota);
    }

    public enum Permiso { INFORMACION, AUTORIZAR, PAGOS }

    /** ¿La persona tiene alguno de estos permisos sobre la mascota ahora? El propietario principal siempre los tiene;
     * las demás personas, solo por un vínculo vigente de su misma empresa que lo incluya. */
    public boolean puede(Apoderado persona, Mascota mascota, Permiso... permisos) {
        if (persona == null || mascota == null || mascota.getApoderado() == null
                || !Boolean.TRUE.equals(persona.getEstado())) {
            return false;
        }
        Integer companyId = companyIdOf(mascota.getApoderado());
        if (companyId == null || !companyId.equals(companyIdOf(persona))) return false;
        if (Objects.equals(mascota.getApoderado().getId(), persona.getId())) return true;
        return relacionRepository.findVigentesDeLaPersonaYMascota(persona.getId(), mascota.getId(), companyId, AppClock.today())
                .stream().anyMatch(relacion -> incluye(relacion, permisos));
    }

    public void exigir(Apoderado persona, Mascota mascota, String mensaje, Permiso... permisos) {
        if (!puede(persona, mascota, permisos)) {
            throw new org.springframework.security.access.AccessDeniedException(mensaje);
        }
    }

    /** Mascotas sobre las que la persona tiene alguno de estos permisos, ordenadas por nombre. */
    public List<Mascota> mascotasConAlgunPermiso(Apoderado persona, Permiso... permisos) {
        Integer companyId = companyIdOf(persona);
        if (companyId == null || !Boolean.TRUE.equals(persona.getEstado())) return List.of();
        Map<Long, Mascota> resultado = new LinkedHashMap<>();
        mascotaRepository.findByApoderadoIdAndCompanyId(persona.getId(), companyId)
                .forEach(mascota -> resultado.put(mascota.getId(), mascota));
        relacionRepository.findVigentesDeLaPersona(persona.getId(), companyId, AppClock.today()).stream()
                .filter(relacion -> incluye(relacion, permisos))
                .forEach(relacion -> resultado.putIfAbsent(relacion.getMascota().getId(), relacion.getMascota()));
        return resultado.values().stream()
                .sorted(java.util.Comparator.comparing(m -> m.getNombreCompleto() == null ? "" : m.getNombreCompleto(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean tieneVinculoVigente(Apoderado persona) {
        Integer companyId = companyIdOf(persona);
        return companyId != null && relacionRepository.existsVigenteDeLaPersona(persona.getId(), companyId, AppClock.today());
    }

    /** A quién se le avisa de lo que pasa con la mascota: su propietario principal y cada persona con un vínculo
     * vigente que incluya recibir información, siempre que sigan activas en la empresa. El principal va primero. */
    public List<Apoderado> destinatariosDeAvisos(Mascota mascota) {
        return destinatariosDeAvisos(List.of(mascota)).getOrDefault(mascota.getId(), List.of());
    }

    public Map<Long, List<Apoderado>> destinatariosDeAvisos(Collection<Mascota> mascotas) {
        Map<Long, List<Apoderado>> resultado = new LinkedHashMap<>();
        Map<Integer, List<Mascota>> porEmpresa = new LinkedHashMap<>();
        for (Mascota mascota : mascotas) {
            Apoderado principal = mascota.getApoderado();
            Integer companyId = companyIdOf(principal);
            if (companyId == null) continue;
            List<Apoderado> destinatarios = new ArrayList<>();
            if (Boolean.TRUE.equals(principal.getEstado())) destinatarios.add(principal);
            resultado.put(mascota.getId(), destinatarios);
            porEmpresa.computeIfAbsent(companyId, ignored -> new ArrayList<>()).add(mascota);
        }
        porEmpresa.forEach((companyId, deLaEmpresa) -> {
            relacionRepository.findVigentesDeLasMascotas(deLaEmpresa.stream().map(Mascota::getId).toList(), companyId, AppClock.today())
                    .stream()
                    .filter(relacion -> Boolean.TRUE.equals(relacion.getPuedeRecibirInformacion()))
                    .forEach(relacion -> {
                        List<Apoderado> destinatarios = resultado.get(relacion.getMascota().getId());
                        boolean yaEsta = destinatarios.stream().anyMatch(d -> d.getId().equals(relacion.getApoderado().getId()));
                        if (!yaEsta) destinatarios.add(relacion.getApoderado());
                    });
        });
        return resultado;
    }

    private boolean incluye(MascotaPersonaRelacion relacion, Permiso... permisos) {
        for (Permiso permiso : permisos) {
            boolean tiene = switch (permiso) {
                case INFORMACION -> Boolean.TRUE.equals(relacion.getPuedeRecibirInformacion());
                case AUTORIZAR -> Boolean.TRUE.equals(relacion.getPuedeAutorizarAtencion());
                case PAGOS -> Boolean.TRUE.equals(relacion.getPuedeRealizarPagos());
            };
            if (tiene) return true;
        }
        return false;
    }

    public boolean hasActiveAuthorizer(Mascota mascota) {
        Apoderado principal = mascota.getApoderado();
        if (principal == null) return false;
        if (Boolean.TRUE.equals(principal.getEstado())) return true;
        Integer companyId = companyIdOf(principal);
        return companyId != null
                && relacionRepository.existsActiveAuthorizer(mascota.getId(), companyId, AppClock.today());
    }

    public void assertCanBeReactivated(Mascota mascota) {
        if (!hasActiveAuthorizer(mascota)) {
            throw new IllegalArgumentException(
                    "No se puede reactivar la mascota porque no tiene un propietario activo. "
                            + "Reactiva primero al propietario, transfiere la titularidad a un cliente activo "
                            + "o vincula a un copropietario que pueda autorizar la atención.");
        }
    }

    public void assertOperable(Mascota mascota) {
        if (!Boolean.TRUE.equals(mascota.getActivo())) {
            throw new IllegalArgumentException("La mascota " + mascota.getNombreCompleto() + " está inactiva");
        }
        if (!hasActiveAuthorizer(mascota)) {
            throw new IllegalArgumentException("La mascota " + mascota.getNombreCompleto()
                    + " no tiene un propietario activo que autorice su atención");
        }
    }

    /**
     * Vuelve a evaluar una mascota después de cambiar quién puede autorizarla: la pausa si ya no tiene a nadie
     * activo (salvo que tenga citas vigentes) o la restaura si el sistema la había pausado y ahora sí lo tiene.
     * Devuelve un aviso para el usuario o null si no hay nada que informar.
     */
    public String reevaluate(Mascota pet) {
        Apoderado principal = pet.getApoderado();
        Integer companyId = companyIdOf(principal);
        if (companyId == null) return null;
        entityManager.flush();
        lockPet(pet);
        boolean operable = hasActiveAuthorizer(pet);
        boolean activa = Boolean.TRUE.equals(pet.getActivo());
        boolean pausadaPorElSistema = pet.getMotivoBaja() != null && pet.getMotivoBaja().esAutomatico();
        LocalDateTime now = AppClock.now();
        if (operable) {
            if (!activa && pausadaPorElSistema) {
                restore(pet, companyId, now);
                return "La mascota " + pet.getNombreCompleto()
                        + " se reactivó porque ya tiene una persona activa que puede autorizar su atención.";
            }
            return null;
        }
        if (!activa) return null;
        if (citaRepository.existsCitaVigenteByMascotaId(pet.getId(), now)) {
            return "La mascota " + pet.getNombreCompleto()
                    + " ya no tiene una persona activa que pueda autorizar su atención, pero sigue activa porque tiene citas vigentes. "
                    + "Vincula a otra persona o transfiere la titularidad.";
        }
        pause(pet, marca(principal.getTipoInactividad()), companyId, now);
        return "La mascota " + pet.getNombreCompleto()
                + " quedó inactiva porque ya no tiene una persona activa que pueda autorizar su atención.";
    }

    /** Revisión periódica: pausa las mascotas activas cuyo propietario está inactivo y que no tienen a nadie más que las autorice. */
    @org.springframework.transaction.annotation.Transactional
    public long reviewInactiveOwners(long afterId, int limit) {
        java.util.List<Mascota> batch = mascotaRepository.findActiveWithInactiveOwner(
                afterId, org.springframework.data.domain.PageRequest.of(0, limit));
        long last = afterId;
        for (Mascota pet : batch) {
            reevaluate(pet);
            last = pet.getId();
        }
        return batch.size() < limit ? -1 : last;
    }

    /**
     * Ajusta las mascotas de la persona después de cambiar su estado. {@code tipoSiSePausa} es el tipo
     * con el que se marcan las que se pausen; es null cuando la persona se reactiva.
     */
    public ApoderadoEstadoResponse syncPets(Apoderado persona, TipoInactividad tipoSiSePausa) {
        ApoderadoEstadoResponse result = new ApoderadoEstadoResponse();
        Integer companyId = companyIdOf(persona);
        if (companyId == null) return result;

        Map<Long, Mascota> pets = new LinkedHashMap<>();
        mascotaRepository.findByApoderadoIdAndCompanyId(persona.getId(), companyId)
                .forEach(pet -> pets.put(pet.getId(), pet));
        relacionRepository.findPetsAuthorizedBy(persona.getId(), companyId, AppClock.today())
                .forEach(pet -> pets.putIfAbsent(pet.getId(), pet));

        pets.values().stream()
                .sorted(java.util.Comparator.comparing(Mascota::getId))
                .forEach(this::lockPet);

        boolean personaActiva = Boolean.TRUE.equals(persona.getEstado());
        LocalDateTime now = AppClock.now();
        MotivoBajaMascota marcaNueva = tipoSiSePausa == null ? null : marca(tipoSiSePausa);

        for (Mascota pet : pets.values()) {
            if (!Objects.equals(companyIdOf(pet.getApoderado()), companyId)) continue;

            boolean operable = hasActiveAuthorizer(pet);
            boolean pausadaPorElSistema = pet.getMotivoBaja() != null && pet.getMotivoBaja().esAutomatico();
            boolean activa = Boolean.TRUE.equals(pet.getActivo());
            String nombre = pet.getNombreCompleto();

            if (operable) {
                if (!activa && pausadaPorElSistema) {
                    restore(pet, companyId, now);
                    result.getMascotasRestauradas().add(nombre);
                } else if (activa && !personaActiva && esPrincipal(pet, persona)) {
                    result.getMascotasQueSiguenActivas().add(nombre);
                } else if (!activa && !pausadaPorElSistema && marcaNueva == null) {
                    result.getMascotasQueSiguenInactivas().add(nombre + " (" + motivoLegible(pet) + ")");
                }
            } else if (activa) {
                if (marcaNueva == null) continue;
                if (citaRepository.existsCitaVigenteByMascotaId(pet.getId(), now)) {
                    result.getMascotasConCitasVigentes().add(nombre);
                } else {
                    pause(pet, marcaNueva, companyId, now);
                    result.getMascotasPausadas().add(nombre);
                }
            } else if (pausadaPorElSistema && marcaNueva != null && pet.getMotivoBaja() != marcaNueva) {
                pet.setMotivoBaja(marcaNueva);
                mascotaRepository.save(pet);
            }
        }
        return result;
    }

    private void pause(Mascota pet, MotivoBajaMascota marca, Integer companyId, LocalDateTime now) {
        pet.setActivo(false);
        pet.setMotivoBaja(marca);
        pet.setOtroMotivoBaja(null);
        pet.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        pet.setFechaModificacionEstado(now);
        mascotaRepository.save(pet);
        auditLogService.log(companyId, "DESACTIVAR_MASCOTA", "Mascotas",
                "Se desactivó a la mascota " + pet.getNombreCompleto()
                        + " porque ninguna persona activa puede autorizar su atención ("
                        + (marca == MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO ? "propietario suspendido" : "propietario dado de baja")
                        + ")");
    }

    private void restore(Mascota pet, Integer companyId, LocalDateTime now) {
        pet.setActivo(true);
        pet.setMotivoBaja(null);
        pet.setOtroMotivoBaja(null);
        pet.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        pet.setFechaModificacionEstado(now);
        mascotaRepository.save(pet);
        auditLogService.log(companyId, "ACTIVAR_MASCOTA", "Mascotas",
                "Se reactivó a la mascota " + pet.getNombreCompleto()
                        + " porque su propietario volvió a estar activo");
    }

    private String motivoLegible(Mascota pet) {
        if (pet.getMotivoBaja() == null) return "sin motivo registrado";
        return switch (pet.getMotivoBaja()) {
            case FALLECIMIENTO -> "fallecimiento";
            case DEJA_ASISTIR -> "dejó de asistir";
            case CAMBIO_PROPIETARIO -> "cambio de propietario";
            case OTRO -> "otro motivo";
            case SUSPENSION_DEL_PROPIETARIO, BAJA_DEL_PROPIETARIO -> "propietario inactivo";
        };
    }

    private MotivoBajaMascota marca(TipoInactividad tipo) {
        return tipo == TipoInactividad.SUSPENSION
                ? MotivoBajaMascota.SUSPENSION_DEL_PROPIETARIO
                : MotivoBajaMascota.BAJA_DEL_PROPIETARIO;
    }

    private boolean esPrincipal(Mascota pet, Apoderado persona) {
        return pet.getApoderado() != null && Objects.equals(pet.getApoderado().getId(), persona.getId());
    }

    private Integer companyIdOf(Apoderado apoderado) {
        return apoderado != null && apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
    }
}
