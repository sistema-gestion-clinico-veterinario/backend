package veterinaria.vargasvet.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.EstadoAccesoPortalPropietario;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.ModoAccesoPropietario;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.RealtimeSubscriptionGuard;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({PortalPropietarioPolicyService.class, SessionSecurityService.class})
class PortalPropietarioPolicyIntegrationTest {

    private static final String ADMIN = "admin@clinica.test";

    @Autowired private PortalPropietarioPolicyService politica;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ApoderadoRepository apoderadoRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository resetTokenRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UsuarioPorRolRepository asignaciones;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @MockitoBean private RealtimeSubscriptionGuard conexionesEnTiempoReal;

    private Company clinica;
    private Company otraClinica;

    @BeforeEach
    void datos() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN, null, List.of()));
        clinica = crearClinica();
        otraClinica = crearClinica();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revocaElPortalDeTodosLosPropietariosDeLaEmpresaYDevuelveCuantos() {
        Usuario conSesion = usuario();
        Usuario sinSesion = usuario();
        Usuario sinPortal = usuario();
        apoderado(conSesion, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        apoderado(sinSesion, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        apoderado(sinPortal, clinica, EstadoAccesoPortalPropietario.SIN_ACCESO);
        apoderado(usuario(), otraClinica, EstadoAccesoPortalPropietario.ACTIVO);
        int usuariosAntes = contar("usuario");
        int apoderadosAntes = contar("apoderado");

        int afectados = cambiarAGestionInterna(clinica);

        assertThat(afectados).isEqualTo(3);
        assertThat(jdbcTemplate.queryForList("SELECT estado_acceso_portal || ':' || acceso_portal_modificado_por || ':' || estado FROM apoderado "
                + "WHERE company_id = ?", String.class, clinica.getId()))
                .containsExactly("REVOCADO:" + ADMIN + ":TRUE", "REVOCADO:" + ADMIN + ":TRUE", "REVOCADO:" + ADMIN + ":TRUE");
        assertThat(jdbcTemplate.queryForList("SELECT estado_acceso_portal FROM apoderado WHERE company_id = ?",
                String.class, otraClinica.getId())).containsExactly("ACTIVO");
        assertThat(jdbcTemplate.queryForObject("SELECT acceso_portal_modificado_por FROM apoderado WHERE company_id = ?",
                String.class, otraClinica.getId())).isNull();
        assertThat(contar("usuario")).isEqualTo(usuariosAntes);
        assertThat(contar("apoderado")).isEqualTo(apoderadosAntes);
    }

    @Test
    void anotaLaFechaDeLaRevocacionComoFechaDeModificacionDelPropietario() {
        apoderado(usuario(), clinica, EstadoAccesoPortalPropietario.ACTIVO);

        cambiarAGestionInterna(clinica);

        assertThat(jdbcTemplate.queryForObject("SELECT acceso_portal_modificado_en IS NOT NULL AND acceso_portal_modificado_en = updated_at "
                + "FROM apoderado WHERE company_id = ?", Boolean.class, clinica.getId())).isTrue();
    }

    @Test
    void cierraLasSesionesActivasDeLosPropietariosSoloEnEsaEmpresa() {
        Usuario propietario = usuario();
        apoderado(propietario, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        apoderado(propietario, otraClinica, EstadoAccesoPortalPropietario.ACTIVO);
        Usuario soloPersonal = usuario();
        Instant revocadaAntes = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        sesion(propietario, clinica, "propietario-activa-1", null);
        sesion(propietario, clinica, "propietario-activa-2", null);
        sesion(propietario, clinica, "propietario-ya-revocada", revocadaAntes);
        sesion(propietario, otraClinica, "propietario-otra-empresa", null);
        sesion(soloPersonal, clinica, "personal-sin-portal", null);

        cambiarAGestionInterna(clinica);

        assertThat(sesionesRevocadas()).containsExactlyInAnyOrder("propietario-activa-1", "propietario-activa-2", "propietario-ya-revocada");
        assertThat(jdbcTemplate.queryForObject("SELECT revoked_at FROM refresh_tokens WHERE family_id = 'propietario-ya-revocada'",
                java.sql.Timestamp.class).toInstant()).isEqualTo(revocadaAntes);
    }

    @Test
    void cierraLaConexionEnTiempoRealDeCadaSesionRevocadaUnaSolaVez() {
        Usuario propietario = usuario();
        apoderado(propietario, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        Usuario soloPersonal = usuario();
        sesion(propietario, clinica, "familia-1", null);
        sesion(propietario, clinica, "familia-1", null);
        sesion(propietario, clinica, "familia-2", null);
        sesion(propietario, clinica, "familia-revocada", Instant.now().minusSeconds(60));
        sesion(propietario, otraClinica, "familia-otra-empresa", null);
        sesion(soloPersonal, clinica, "familia-personal", null);

        cambiarAGestionInterna(clinica);

        verify(conexionesEnTiempoReal, times(1)).closeConnectionsOfSession("familia-1");
        verify(conexionesEnTiempoReal, times(1)).closeConnectionsOfSession("familia-2");
        verifyNoMoreInteractions(conexionesEnTiempoReal);
    }

    @Test
    void sinSesionesActivasNoCierraConexionesNiFalla() {
        apoderado(usuario(), clinica, EstadoAccesoPortalPropietario.ACTIVO);

        int afectados = cambiarAGestionInterna(clinica);

        assertThat(afectados).isEqualTo(1);
        verify(conexionesEnTiempoReal, never()).closeConnectionsOfSession(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void borraLosTokensDeRecuperacionDeLosPropietariosSoloEnEsaEmpresa() {
        Usuario propietario = usuario();
        apoderado(propietario, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        apoderado(propietario, otraClinica, EstadoAccesoPortalPropietario.ACTIVO);
        Usuario soloPersonal = usuario();
        recuperacion(propietario, clinica, "del-propietario-en-la-empresa");
        recuperacion(propietario, clinica, "otro-del-propietario");
        recuperacion(propietario, otraClinica, "del-propietario-en-otra-empresa");
        recuperacion(soloPersonal, clinica, "del-personal");

        cambiarAGestionInterna(clinica);

        assertThat(jdbcTemplate.queryForList("SELECT token FROM password_reset_tokens", String.class))
                .containsExactlyInAnyOrder("del-propietario-en-otra-empresa", "del-personal");
    }

    @Test
    void limpiaLaInvitacionDeQuienNoTieneRolLaboralYConservaLaDeQuienSi() {
        Role personal = rol(clinica, RoleScope.STAFF, true);
        Role personalInactivo = rol(clinica, RoleScope.STAFF, false);
        Role personalDeOtraEmpresa = rol(otraClinica, RoleScope.STAFF, true);
        Role portal = rol(clinica, RoleScope.CLIENT, true);

        Usuario sinRol = conInvitacion("sin-rol");
        Usuario conRolLaboral = conInvitacion("con-rol-laboral");
        Usuario rolLaboralInactivo = conInvitacion("rol-laboral-inactivo");
        Usuario rolLaboralEnOtraEmpresa = conInvitacion("rol-laboral-otra-empresa");
        Usuario soloRolDePortal = conInvitacion("solo-rol-de-portal");
        conInvitacion("no-propietario");
        for (Usuario propietario : List.of(sinRol, conRolLaboral, rolLaboralInactivo, rolLaboralEnOtraEmpresa, soloRolDePortal)) {
            apoderado(propietario, clinica, EstadoAccesoPortalPropietario.ACTIVO);
        }
        asignar(conRolLaboral, personal, clinica);
        asignar(rolLaboralInactivo, personalInactivo, clinica);
        asignar(rolLaboralEnOtraEmpresa, personalDeOtraEmpresa, otraClinica);
        asignar(soloRolDePortal, portal, clinica);

        cambiarAGestionInterna(clinica);

        assertThat(usuariosConInvitacion()).containsExactlyInAnyOrder("con-rol-laboral", "no-propietario");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuario WHERE verification_token_expires_at IS NOT NULL",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void elNumeroDeSentenciasNoDependeDeCuantosPropietariosTengaLaEmpresa() {
        for (int i = 0; i < 3; i++) {
            propietarioCompleto(clinica);
        }
        for (int i = 0; i < 40; i++) {
            propietarioCompleto(otraClinica);
        }
        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        long conTres = sentenciasAlCambiar(clinica, estadisticas);
        long conCuarenta = sentenciasAlCambiar(otraClinica, estadisticas);

        assertThat(conCuarenta).isEqualTo(conTres);
        assertThat(conTres).isLessThanOrEqualTo(6);
        assertThat(sesionesRevocadas()).hasSize(86);
    }

    private long sentenciasAlCambiar(Company empresa, Statistics estadisticas) {
        entityManager.flush();
        estadisticas.clear();
        cambiarAGestionInterna(empresa);
        return estadisticas.getPrepareStatementCount();
    }

    private void propietarioCompleto(Company empresa) {
        Usuario propietario = conInvitacion("propietario-" + UUID.randomUUID());
        apoderado(propietario, empresa, EstadoAccesoPortalPropietario.ACTIVO);
        sesion(propietario, empresa, UUID.randomUUID().toString(), null);
        sesion(propietario, empresa, UUID.randomUUID().toString(), null);
        recuperacion(propietario, empresa, "recuperacion-" + UUID.randomUUID());
    }

    private int cambiarAGestionInterna(Company empresa) {
        return politica.aplicarCambio(empresa, ModoAccesoPropietario.PORTAL_OPCIONAL, ModoAccesoPropietario.SOLO_GESTION_INTERNA);
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private List<String> sesionesRevocadas() {
        return jdbcTemplate.queryForList("SELECT family_id FROM refresh_tokens WHERE revoked_at IS NOT NULL", String.class);
    }

    private List<String> usuariosConInvitacion() {
        return jdbcTemplate.queryForList("SELECT username FROM usuario WHERE verification_token IS NOT NULL", String.class);
    }

    private Company crearClinica() {
        Company company = new Company();
        company.setName("Clinica Patitas");
        company.setSlug("patitas-" + UUID.randomUUID());
        company.setRuc("20" + String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 9));
        company.setActivo(true);
        return companyRepository.save(company);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("persona-" + UUID.randomUUID() + "@patitas.test");
        usuario.setUsername("persona-" + UUID.randomUUID());
        usuario.setNombre("Persona");
        usuario.setApellido("Test");
        usuario.setDni(String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits())).substring(0, 8));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        usuario.setCompany(clinica);
        return usuarioRepository.save(usuario);
    }

    private Usuario conInvitacion(String username) {
        Usuario usuario = usuario();
        usuario.setUsername(username);
        usuario.setVerificationToken("invitacion-" + username);
        usuario.setVerificationTokenExpiresAt(LocalDateTime.now().plusDays(1));
        return usuarioRepository.save(usuario);
    }

    private Apoderado apoderado(Usuario usuario, Company empresa, EstadoAccesoPortalPropietario acceso) {
        Apoderado apoderado = new Apoderado();
        apoderado.setUser(usuario);
        apoderado.setCompany(empresa);
        apoderado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        apoderado.setNumeroDocumento(usuario.getDni());
        apoderado.setGenero(Genero.FEMENINO);
        apoderado.setEstadoAccesoPortal(acceso);
        return apoderadoRepository.save(apoderado);
    }

    private void sesion(Usuario usuario, Company empresa, String familia, Instant revocadaEn) {
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash("hash-" + UUID.randomUUID()).jti(UUID.randomUUID().toString()).familyId(familia)
                .expiryDate(Instant.now().plusSeconds(3600)).sessionStartedAt(Instant.now())
                .revokedAt(revocadaEn).usuario(usuario).company(empresa).build());
    }

    private void recuperacion(Usuario usuario, Company empresa, String token) {
        resetTokenRepository.save(PasswordResetToken.builder().token(token)
                .expiryDate(LocalDateTime.now().plusHours(1)).usuario(usuario).company(empresa).build());
    }

    private Role rol(Company empresa, RoleScope alcance, boolean activo) {
        Role rol = new Role();
        rol.setName("Rol " + UUID.randomUUID());
        rol.setScope(alcance);
        rol.setPurpose(RolePurpose.CUSTOM);
        rol.setActivo(activo);
        rol.setCompany(empresa);
        return roleRepository.save(rol);
    }

    private void asignar(Usuario usuario, Role rol, Company empresa) {
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(rol);
        asignacion.setCompany(empresa);
        asignaciones.save(asignacion);
    }
}
