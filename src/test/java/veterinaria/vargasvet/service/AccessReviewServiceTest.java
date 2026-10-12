package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.AccessReview;
import veterinaria.vargasvet.domain.entity.AccessReviewItem;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.AccessReviewDecision;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.AccessReviewRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessReviewServiceTest {

    private static final int COMPANY = 7;

    @Mock private UsuarioPorRolRepository assignmentRepository;
    @Mock private RolVistaPermisoRepository permissionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AccessReviewRepository reviewRepository;
    @Mock private AccessReviewItemRepository itemRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private RoleAssignmentService roleAssignmentService;
    @Mock private AccesoValidator accesoValidator;
    @Mock private AuditLogService auditLogService;

    private AccessReviewService service;
    private Company company;
    private Role admin;
    private Role recepcion;
    private Role veterinario;
    private Usuario ana;
    private Usuario beto;

    @BeforeEach
    void setUp() {
        service = new AccessReviewService(assignmentRepository, permissionRepository, refreshTokenRepository,
                reviewRepository, itemRepository, companyRepository, roleAssignmentService, accesoValidator,
                auditLogService);
        company = new Company();
        company.setId(COMPANY);
        admin = role(1, "ROLE_ADMIN", RolePurpose.COMPANY_ADMIN);
        recepcion = role(2, "Recepción", RolePurpose.CUSTOM);
        veterinario = role(3, "Veterinario", RolePurpose.CUSTOM);
        ana = usuario(10, "Ana", "Paredes");
        beto = usuario(11, "Beto", "Quispe");

        lenient().when(permissionRepository.findByRolIdWithVistaAndVentana(1)).thenReturn(List.of());
        lenient().when(permissionRepository.findByRolIdWithVistaAndVentana(2))
                .thenReturn(List.of(permission(recepcion, "VISTA_ROLES", "Roles", true)));
        lenient().when(permissionRepository.findByRolIdWithVistaAndVentana(3))
                .thenReturn(List.of(permission(veterinario, "VISTA_HISTORIAS", "Historias clínicas", true)));
        lenient().when(refreshTokenRepository.ultimaActividadPorPersona(anyCollection(), eq(COMPANY))).thenReturn(List.of());
        lenient().when(companyRepository.lockById(COMPANY)).thenReturn(Optional.of(company));
        lenient().when(reviewRepository.save(any(AccessReview.class))).thenAnswer(invocation -> {
            AccessReview review = invocation.getArgument(0);
            review.setId(5L);
            return review;
        });
    }

    @Test
    void listaSoloLosAccesosPrivilegiadosYExplicaElMotivo() {
        stubAssignments(
                assignment(100, ana, admin),
                assignment(101, beto, recepcion),
                assignment(102, usuario(12, "Carla", "Rojas"), veterinario));

        AccessReviewOverviewDTO overview = enEmpresa(() -> service.overview(COMPANY));

        assertThat(overview.items()).extracting(AccessReviewItemDTO::assignmentId).containsExactly(100, 101);
        assertThat(overview.items().get(0).reasons()).containsExactly("Administrador de la empresa");
        assertThat(overview.items().get(1).reasons()).containsExactly("Puede modificar: Roles");
    }

    @Test
    void senalaAQuienTieneVariosRolesYSinActividadReciente() {
        stubAssignments(assignment(100, ana, admin), assignment(103, ana, veterinario));
        when(refreshTokenRepository.ultimaActividadPorPersona(anyCollection(), eq(COMPANY)))
                .thenReturn(List.of(actividad(10, Instant.now().minus(Duration.ofDays(200)))));

        AccessReviewOverviewDTO overview = enEmpresa(() -> service.overview(COMPANY));

        assertThat(overview.items()).hasSize(1);
        assertThat(overview.items().getFirst().reasons())
                .contains("Tiene 2 roles de personal en la empresa")
                .anyMatch(reason -> reason.startsWith("Sin actividad desde "));
    }

    @Test
    void noSenalaInactividadCuandoLaUltimaActividadEsReciente() {
        stubAssignments(assignment(100, ana, admin));
        when(refreshTokenRepository.ultimaActividadPorPersona(anyCollection(), eq(COMPANY)))
                .thenReturn(List.of(actividad(10, Instant.now().minus(Duration.ofDays(3)))));

        AccessReviewOverviewDTO overview = enEmpresa(() -> service.overview(COMPANY));

        assertThat(overview.items().getFirst().reasons()).containsExactly("Administrador de la empresa");
    }

    @Test
    void sinRevisionPreviaLaRevisionEstaVencida() {
        stubAssignments(assignment(100, ana, admin));
        when(reviewRepository.findFirstByCompanyIdOrderByPerformedAtDescIdDesc(COMPANY)).thenReturn(Optional.empty());

        AccessReviewOverviewDTO overview = enEmpresa(() -> service.overview(COMPANY));

        assertThat(overview.overdue()).isTrue();
        assertThat(overview.nextDueAt()).isNull();
        assertThat(overview.lastReview()).isNull();
    }

    @Test
    void unaRevisionRecienteNoEstaVencidaYUnaAntiguaSi() {
        stubAssignments(assignment(100, ana, admin));
        when(reviewRepository.findFirstByCompanyIdOrderByPerformedAtDescIdDesc(COMPANY))
                .thenReturn(Optional.of(revisionHace(10)));
        assertThat(enEmpresa(() -> service.overview(COMPANY)).overdue()).isFalse();

        when(reviewRepository.findFirstByCompanyIdOrderByPerformedAtDescIdDesc(COMPANY))
                .thenReturn(Optional.of(revisionHace(120)));
        assertThat(enEmpresa(() -> service.overview(COMPANY)).overdue()).isTrue();
    }

    @Test
    void exigeDecidirSobreTodosLosAccesosPrivilegiados() {
        stubAssignments(assignment(100, ana, admin), assignment(101, beto, recepcion));

        assertThatThrownBy(() -> enEmpresa(() -> service.register(COMPANY, request(keep(100)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Falta decidir sobre 1 acceso");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rechazaUnAccesoQueYaNoEstaVigente() {
        stubAssignments(assignment(100, ana, admin));

        assertThatThrownBy(() -> enEmpresa(() -> service.register(COMPANY, request(keep(100), keep(999)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya no está vigente");
    }

    @Test
    void rechazaDecisionesRepetidasParaElMismoAcceso() {
        stubAssignments(assignment(100, ana, admin));

        assertThatThrownBy(() -> enEmpresa(() -> service.register(COMPANY, request(keep(100), keep(100)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("más de una decisión");
    }

    @Test
    void retirarUnAccesoExigeElMotivo() {
        stubAssignments(assignment(101, beto, recepcion));

        assertThatThrownBy(() -> enEmpresa(() -> service.register(COMPANY, request(revoke(101, "  ")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");

        verify(roleAssignmentService, never()).replaceStaffRoles(any(), any(), any());
    }

    @Test
    void retirarQuitaSoloEseRolConElServicioDeAsignaciones() {
        UsuarioPorRol privilegiada = assignment(101, beto, recepcion);
        stubAssignments(privilegiada, assignment(104, beto, veterinario));
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(11, COMPANY, RoleScope.STAFF))
                .thenReturn(List.of(privilegiada, assignment(104, beto, veterinario)));

        AccessReviewSummaryDTO result = enEmpresa(() ->
                service.register(COMPANY, request(revoke(101, "Ya no atiende recepción"))));

        verify(roleAssignmentService).replaceStaffRoles(beto, company, Set.of(3));
        assertThat(result.revokedCount()).isEqualTo(1);
        assertThat(result.keptCount()).isZero();
    }

    @Test
    void noRetiraElUnicoRolDePersonalDeUnaPersona() {
        UsuarioPorRol unica = assignment(101, beto, recepcion);
        stubAssignments(unica);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(11, COMPANY, RoleScope.STAFF))
                .thenReturn(List.of(unica));

        assertThatThrownBy(() -> enEmpresa(() ->
                service.register(COMPANY, request(revoke(101, "Ya no trabaja aquí")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("único rol de personal")
                .hasMessageContaining("Suspenda la cuenta");

        verify(roleAssignmentService, never()).replaceStaffRoles(any(), any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void mantenerNoModificaRolesYGuardaLaEvidenciaDeLaRevision() {
        stubAssignments(assignment(100, ana, admin), assignment(101, beto, recepcion));

        AccessReviewSummaryDTO result = enEmpresa(() -> service.register(COMPANY,
                new AccessReviewRequest(List.of(keep(100), keep(101)), "Revisión trimestral")));

        verify(accesoValidator).validarModificar("VISTA_ROLES");
        verify(roleAssignmentService, never()).replaceStaffRoles(any(), any(), any());
        assertThat(result.reviewedCount()).isEqualTo(2);
        assertThat(result.keptCount()).isEqualTo(2);
        assertThat(result.revokedCount()).isZero();
        assertThat(result.performedByEmail()).isEqualTo("revisor@clinica.test");

        ArgumentCaptor<AccessReview> review = ArgumentCaptor.forClass(AccessReview.class);
        verify(reviewRepository).save(review.capture());
        assertThat(review.getValue().getCompanyId()).isEqualTo(COMPANY);
        assertThat(review.getValue().getNotes()).isEqualTo("Revisión trimestral");
        assertThat(review.getValue().getPerformedByUserId()).isEqualTo(99);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccessReviewItem>> items = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(items.capture());
        assertThat(items.getValue()).hasSize(2)
                .allSatisfy(item -> assertThat(item.getReview().getId()).isEqualTo(5L))
                .extracting(AccessReviewItem::getUserName).containsExactly("Ana Paredes", "Beto Quispe");
        verify(auditLogService).log(COMPANY, "REVISAR_ACCESOS_PRIVILEGIADOS", "Roles",
                "Revisión de accesos privilegiados: 2 revisados, 2 mantenidos, 0 retirados");
    }

    @Test
    void guardaElMotivoYElResultadoDeCadaAccesoRetirado() {
        UsuarioPorRol privilegiada = assignment(101, beto, recepcion);
        UsuarioPorRol otra = assignment(104, beto, veterinario);
        stubAssignments(assignment(100, ana, admin), privilegiada, otra);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(11, COMPANY, RoleScope.STAFF))
                .thenReturn(List.of(privilegiada, otra));

        enEmpresa(() -> service.register(COMPANY, request(keep(100), revoke(101, "Cambió de puesto"))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccessReviewItem>> items = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(items.capture());
        AccessReviewItem retirado = items.getValue().stream()
                .filter(item -> item.getUsuarioId() == 11).findFirst().orElseThrow();
        assertThat(retirado.getDecision()).isEqualTo(AccessReviewDecision.REVOKE);
        assertThat(retirado.getComment()).isEqualTo("Cambió de puesto");
        assertThat(retirado.getReasons()).contains("Puede modificar: Roles");
    }

    @Test
    void noPermiteRevisarAccesosDeOtraEmpresa() {
        assertThatThrownBy(() -> enEmpresa(() -> service.overview(8)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> enEmpresa(() -> service.register(8, request(keep(1)))))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> enEmpresa(() -> service.history(8))).isInstanceOf(AccessDeniedException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void elDetalleDeUnaRevisionDeOtraEmpresaNoSeEncuentra() {
        AccessReview ajena = revisionHace(5);
        ajena.setCompanyId(99);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> enEmpresa(() -> service.detail(COMPANY, 5L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubAssignments(UsuarioPorRol... assignments) {
        when(assignmentRepository.findActiveAssignmentsByCompanyIdAndRoleScope(COMPANY, RoleScope.STAFF))
                .thenReturn(List.of(assignments));
    }

    private <T> T enEmpresa(Supplier<T> action) {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(COMPANY);
            security.when(SecurityUtils::getCurrentUserId).thenReturn(99);
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("revisor@clinica.test");
            return action.get();
        }
    }

    private AccessReviewRequest request(AccessReviewRequest.Decision... decisions) {
        return new AccessReviewRequest(List.of(decisions), null);
    }

    private AccessReviewRequest.Decision keep(int assignmentId) {
        return new AccessReviewRequest.Decision(assignmentId, AccessReviewDecision.KEEP, null);
    }

    private AccessReviewRequest.Decision revoke(int assignmentId, String comment) {
        return new AccessReviewRequest.Decision(assignmentId, AccessReviewDecision.REVOKE, comment);
    }

    private AccessReview revisionHace(int dias) {
        AccessReview review = new AccessReview();
        review.setId(5L);
        review.setCompanyId(COMPANY);
        review.setPerformedAt(AppClock.now().minusDays(dias));
        return review;
    }

    private Usuario usuario(int id, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(nombre.toLowerCase() + "@clinica.test");
        return usuario;
    }

    private Role role(int id, String name, RolePurpose purpose) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(purpose);
        role.setActivo(true);
        return role;
    }

    private UsuarioPorRol assignment(int id, Usuario usuario, Role role) {
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setId(id);
        assignment.setUsuario(usuario);
        assignment.setRol(role);
        assignment.setCompany(company);
        assignment.setCreatedAt(AppClock.now());
        return assignment;
    }

    private RolVistaPermiso permission(Role role, String codigo, String nombre, boolean modificar) {
        Vista vista = new Vista();
        vista.setCodigo(codigo);
        vista.setNombre(nombre);
        RolVistaPermiso permission = new RolVistaPermiso();
        permission.setRol(role);
        permission.setVista(vista);
        permission.setLeer(true);
        permission.setModificar(modificar);
        return permission;
    }

    private RefreshTokenRepository.ActividadDePersona actividad(int usuarioId, Instant ultima) {
        return new RefreshTokenRepository.ActividadDePersona() {
            @Override
            public Integer getUsuarioId() {
                return usuarioId;
            }

            @Override
            public Instant getUltimaActividad() {
                return ultima;
            }
        };
    }
}
