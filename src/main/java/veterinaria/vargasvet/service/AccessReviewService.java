package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AccessReview;
import veterinaria.vargasvet.domain.entity.AccessReviewItem;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.AccessReviewDecision;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.AccessReviewRequest;
import veterinaria.vargasvet.dto.response.AccessReviewDetailDTO;
import veterinaria.vargasvet.dto.response.AccessReviewItemDTO;
import veterinaria.vargasvet.dto.response.AccessReviewOverviewDTO;
import veterinaria.vargasvet.dto.response.AccessReviewSummaryDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.AccessReviewItemRepository;
import veterinaria.vargasvet.repository.AccessReviewRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessReviewService {

    private static final String ROLES_VIEW = "VISTA_ROLES";
    private static final Set<String> ADMIN_VIEWS = Set.of(
            ROLES_VIEW, "VISTA_EMPLEADOS", "VISTA_COMPANY", "VISTA_AUDITORIA_ADMIN", "VISTA_VENTANAS");
    private static final int HISTORY_SIZE = 20;
    private static final int MAX_REASONS_LENGTH = 600;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioPorRolRepository assignmentRepository;
    private final RolVistaPermisoRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessReviewRepository reviewRepository;
    private final AccessReviewItemRepository itemRepository;
    private final CompanyRepository companyRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final AccesoValidator accesoValidator;
    private final AuditLogService auditLogService;

    @Value("${security.access-review.interval-days:90}")
    private int intervalDays = 90;

    @Value("${security.access-review.inactivity-days:90}")
    private int inactivityDays = 90;

    private record Candidate(UsuarioPorRol assignment, List<String> reasons, Instant lastActivity) {
    }

    @Transactional(readOnly = true)
    public AccessReviewOverviewDTO overview(Integer companyId) {
        requireCompanyContext(companyId);
        List<AccessReviewItemDTO> items = candidates(companyId).stream().map(this::toDto).toList();
        Optional<AccessReview> last = reviewRepository.findFirstByCompanyIdOrderByPerformedAtDescIdDesc(companyId);
        LocalDateTime nextDue = last.map(review -> review.getPerformedAt().plusDays(intervalDays)).orElse(null);
        boolean overdue = nextDue == null || AppClock.now().isAfter(nextDue);
        return new AccessReviewOverviewDTO(intervalDays, overdue, nextDue, last.map(this::summary).orElse(null), items);
    }

    @Transactional
    public AccessReviewSummaryDTO register(Integer companyId, AccessReviewRequest request) {
        requireCompanyContext(companyId);
        accesoValidator.validarModificar(ROLES_VIEW);
        Company company = companyRepository.lockById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Map<Integer, Candidate> candidates = candidates(companyId).stream()
                .collect(Collectors.toMap(c -> c.assignment().getId(), c -> c, (a, b) -> a, LinkedHashMap::new));
        Map<Integer, AccessReviewRequest.Decision> decisions = indexDecisions(request, candidates);

        int kept = 0;
        int revoked = 0;
        List<AccessReviewItem> items = new ArrayList<>();
        for (Candidate candidate : candidates.values()) {
            AccessReviewRequest.Decision decision = decisions.get(candidate.assignment().getId());
            if (decision.decision() == AccessReviewDecision.REVOKE) {
                revoke(candidate, company, decision);
                revoked++;
            } else {
                kept++;
            }
            items.add(snapshot(candidate, decision));
        }

        AccessReview review = new AccessReview();
        review.setCompanyId(companyId);
        review.setPerformedAt(AppClock.now());
        review.setPerformedByUserId(SecurityUtils.getCurrentUserId());
        review.setPerformedByEmail(SecurityUtils.getCurrentUserEmail());
        review.setReviewedCount(candidates.size());
        review.setKeptCount(kept);
        review.setRevokedCount(revoked);
        review.setNotes(blankToNull(request.notes()));
        AccessReview saved = reviewRepository.save(review);
        items.forEach(item -> item.setReview(saved));
        itemRepository.saveAll(items);

        auditLogService.log(companyId, "REVISAR_ACCESOS_PRIVILEGIADOS", "Roles",
                "Revisión de accesos privilegiados: " + candidates.size() + " revisados, "
                        + kept + " mantenidos, " + revoked + " retirados");
        return summary(saved);
    }

    @Transactional(readOnly = true)
    public List<AccessReviewSummaryDTO> history(Integer companyId) {
        requireCompanyContext(companyId);
        return reviewRepository.findByCompanyIdOrderByPerformedAtDescIdDesc(companyId, PageRequest.of(0, HISTORY_SIZE))
                .stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public AccessReviewDetailDTO detail(Integer companyId, Long reviewId) {
        requireCompanyContext(companyId);
        AccessReview review = reviewRepository.findById(reviewId)
                .filter(found -> Objects.equals(found.getCompanyId(), companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Revisión no encontrada"));
        List<AccessReviewDetailDTO.Entry> entries = itemRepository.findByReviewIdOrderByIdAsc(review.getId()).stream()
                .map(item -> new AccessReviewDetailDTO.Entry(item.getUserName(), item.getUserEmail(),
                        item.getRoleName(), item.getReasons(), item.getDecision(), item.getComment()))
                .toList();
        return new AccessReviewDetailDTO(summary(review), entries);
    }

    private Map<Integer, AccessReviewRequest.Decision> indexDecisions(
            AccessReviewRequest request, Map<Integer, Candidate> candidates) {
        Map<Integer, AccessReviewRequest.Decision> decisions = new HashMap<>();
        for (AccessReviewRequest.Decision decision : request.decisions()) {
            if (!candidates.containsKey(decision.assignmentId())) {
                throw new IllegalArgumentException(
                        "Uno de los accesos ya no está vigente. Actualice la revisión e inténtelo de nuevo");
            }
            if (decisions.put(decision.assignmentId(), decision) != null) {
                throw new IllegalArgumentException("Hay más de una decisión para el mismo acceso");
            }
            if (decision.decision() == AccessReviewDecision.REVOKE && isBlank(decision.comment())) {
                throw new IllegalArgumentException("Indique el motivo para retirar cada acceso");
            }
        }
        long missing = candidates.keySet().stream().filter(id -> !decisions.containsKey(id)).count();
        if (missing > 0) {
            throw new IllegalArgumentException("Falta decidir sobre " + missing
                    + " acceso(s). La revisión debe cubrir todos los accesos privilegiados vigentes");
        }
        return decisions;
    }

    private void revoke(Candidate candidate, Company company, AccessReviewRequest.Decision decision) {
        Usuario usuario = candidate.assignment().getUsuario();
        Integer roleId = candidate.assignment().getRol().getId();
        Set<Integer> remaining = assignmentRepository
                .findByUsuarioIdAndCompanyIdAndRoleScope(usuario.getId(), company.getId(), RoleScope.STAFF).stream()
                .map(assignment -> assignment.getRol().getId())
                .filter(id -> !id.equals(roleId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (remaining.isEmpty()) {
            throw new IllegalArgumentException("No se puede retirar el único rol de personal de "
                    + fullName(usuario) + ". Suspenda la cuenta si ya no debe tener acceso");
        }
        roleAssignmentService.replaceStaffRoles(usuario, company, remaining);
    }

    private List<Candidate> candidates(Integer companyId) {
        List<UsuarioPorRol> assignments = assignmentRepository
                .findActiveAssignmentsByCompanyIdAndRoleScope(companyId, RoleScope.STAFF);
        Map<Integer, List<String>> privilegesByRole = new HashMap<>();
        assignments.forEach(a -> privilegesByRole.computeIfAbsent(a.getRol().getId(), id -> privilegeReasons(a.getRol())));
        Map<Integer, Long> rolesPerUser = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getUsuario().getId(), Collectors.counting()));
        List<UsuarioPorRol> privileged = assignments.stream()
                .filter(a -> !privilegesByRole.get(a.getRol().getId()).isEmpty())
                .toList();
        Map<Integer, Instant> activity = lastActivity(privileged, companyId);
        Instant inactiveBefore = Instant.now().minus(Duration.ofDays(inactivityDays));

        List<Candidate> result = new ArrayList<>();
        for (UsuarioPorRol assignment : privileged) {
            Integer usuarioId = assignment.getUsuario().getId();
            List<String> reasons = new ArrayList<>(privilegesByRole.get(assignment.getRol().getId()));
            long roleCount = rolesPerUser.getOrDefault(usuarioId, 0L);
            if (roleCount >= 2) {
                reasons.add("Tiene " + roleCount + " roles de personal en la empresa");
            }
            Instant last = activity.get(usuarioId);
            if (last != null && last.isBefore(inactiveBefore)) {
                reasons.add("Sin actividad desde " + DATE.format(last.atZone(LIMA)));
            }
            result.add(new Candidate(assignment, reasons, last));
        }
        return result;
    }

    private List<String> privilegeReasons(Role role) {
        if (role.getPurpose() == RolePurpose.COMPANY_ADMIN) {
            return List.of("Administrador de la empresa");
        }
        return permissionRepository.findByRolIdWithVistaAndVentana(role.getId()).stream()
                .filter(permission -> ADMIN_VIEWS.contains(permission.getVista().getCodigo()))
                .filter(permission -> permission.isEscribir() || permission.isModificar() || permission.isEliminar())
                .map(permission -> "Puede modificar: " + permission.getVista().getNombre())
                .distinct()
                .toList();
    }

    private Map<Integer, Instant> lastActivity(List<UsuarioPorRol> privileged, Integer companyId) {
        Set<Integer> ids = privileged.stream().map(a -> a.getUsuario().getId()).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Instant> activity = new HashMap<>();
        refreshTokenRepository.ultimaActividadPorPersona(ids, companyId).forEach(row -> {
            if (row.getUltimaActividad() != null) {
                activity.put(row.getUsuarioId(), row.getUltimaActividad());
            }
        });
        return activity;
    }

    private AccessReviewItemDTO toDto(Candidate candidate) {
        UsuarioPorRol assignment = candidate.assignment();
        Usuario usuario = assignment.getUsuario();
        return new AccessReviewItemDTO(assignment.getId(), usuario.getId(), fullName(usuario), usuario.getEmail(),
                assignment.getRol().getId(), assignment.getRol().getName(), candidate.reasons(),
                candidate.lastActivity(), assignment.getCreatedAt());
    }

    private AccessReviewItem snapshot(Candidate candidate, AccessReviewRequest.Decision decision) {
        Usuario usuario = candidate.assignment().getUsuario();
        Role role = candidate.assignment().getRol();
        AccessReviewItem item = new AccessReviewItem();
        item.setUsuarioId(usuario.getId());
        item.setUserName(fullName(usuario));
        item.setUserEmail(usuario.getEmail());
        item.setRoleId(role.getId());
        item.setRoleName(role.getName());
        item.setReasons(truncate(String.join("; ", candidate.reasons()), MAX_REASONS_LENGTH));
        item.setDecision(decision.decision());
        item.setComment(blankToNull(decision.comment()));
        return item;
    }

    private AccessReviewSummaryDTO summary(AccessReview review) {
        return new AccessReviewSummaryDTO(review.getId(), review.getPerformedAt(), review.getPerformedByEmail(),
                review.getReviewedCount(), review.getKeptCount(), review.getRevokedCount(), review.getNotes());
    }

    private void requireCompanyContext(Integer companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Debe indicar la empresa");
        }
        if (!SecurityUtils.isSuperAdmin() && !Objects.equals(companyId, SecurityUtils.getCurrentCompanyId())) {
            throw new AccessDeniedException("No puede revisar accesos de otra empresa");
        }
    }

    private String fullName(Usuario usuario) {
        String name = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        return name.isEmpty() ? String.valueOf(usuario.getEmail()) : name;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
