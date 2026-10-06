package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.service.ApoderadoService;
import veterinaria.vargasvet.service.EmailService;
import veterinaria.vargasvet.service.CompanyRoleProvisioningService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.util.BusinessValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.dto.response.ApoderadoEstadoResponse;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApoderadoServiceImpl implements ApoderadoService {

    private static final long INVITATION_RESEND_COOLDOWN_MINUTES = 5;
    private static final long STATE_CHANGE_REPEAT_MINUTES = 2;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final veterinaria.vargasvet.repository.UsuarioPorRolRepository usuarioPorRolRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final BusinessValidator businessValidator;
    private final EmailService emailService;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;
    private final CompanyRoleProvisioningService companyRoleProvisioningService;
    private final SessionSecurityService sessionSecurityService;
    private final veterinaria.vargasvet.service.CompanyMembershipService companyMembershipService;
    private final veterinaria.vargasvet.repository.CitaRepository citaRepository;
    private final veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository credencialRepository;
    private final UsuarioContactoService contactoService;
    private final veterinaria.vargasvet.service.PetOwnershipService petOwnershipService;
    private final veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard;
    private final veterinaria.vargasvet.service.AdministratorProtection administratorProtection;
    private final veterinaria.vargasvet.service.AccessRestoredNotifier accessRestoredNotifier;
    private final veterinaria.vargasvet.service.ConsentimientoDatosService consentimientoDatosService;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

    @Value("${app.company.logo}")
    private String defaultCompanyLogo;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.address}")
    private String companyAddress;

    @Value("${security.verification-token-validity-hours:24}")
    private long verificationTokenValidityHours;

    @Override
    @Transactional
    public UserProfileDTO registerApoderado(ApoderadoRequest dto) {
        dto.setEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT));

        Integer companyIdToUse;
        if (SecurityUtils.isSuperAdmin()) {
            if (dto.getCompanyId() == null) {
                throw new IllegalArgumentException("El Super Admin debe proporcionar un companyId");
            }
            companyIdToUse = dto.getCompanyId();
        } else {
            companyIdToUse = SecurityUtils.getCurrentCompanyId();
            if (companyIdToUse == null) {
                throw new IllegalArgumentException("No se pudo determinar la empresa del registrador");
            }
        }
        businessValidator.checkCompanyActiva(companyIdToUse);
        Company companyToUse = companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        consentimientoDatosService.exigirAltaValida(companyIdToUse, dto.getAvisoInformado());

        // Aislamiento total entre empresas: la busqueda de "ya existe" es SOLO dentro de
        // esta misma empresa (ej. la persona ya es empleado aqui y ahora tambien se
        // registra como cliente, o es un reingreso de un cliente inactivo) - nunca se
        // cruza contra otras empresas, aunque coincida el DNI o el correo.
        java.util.Optional<Usuario> existingUsuario = usuarioRepository.findByEmailAndCompanyId(dto.getEmail(), companyIdToUse)
                .or(() -> usuarioRepository.findByDniAndCompanyId(dto.getNumeroDocumento(), companyIdToUse));
        boolean esUsuarioNuevo = existingUsuario.isEmpty();
        Usuario savedUser;

        if (esUsuarioNuevo) {
            String username = dto.getUsername() == null ? null : dto.getUsername().trim().toLowerCase(java.util.Locale.ROOT);
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("El usuario es obligatorio para una persona nueva");
            }
            if (usuarioRepository.existsByUsernameIgnoreCaseAndCompanyId(username, companyIdToUse)) {
                throw new IllegalArgumentException("El usuario ya está en uso en esta empresa");
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setNombre(dto.getNombre());
            usuario.setApellido(dto.getApellido());
            usuario.setEmail(dto.getEmail());
            usuario.setDni(dto.getNumeroDocumento());
            usuario.setCompany(companyToUse);
            usuario.setActivo(false);
            usuario.setEmailVerified(false);

            savedUser = usuarioRepository.save(usuario);
        } else {
            // Ya existe una identidad EN ESTA MISMA EMPRESA (ej. ya es empleado aqui, o
            // es un cliente inactivo que vuelve) - se reutiliza, nunca se cruza con otra
            // empresa.
            savedUser = existingUsuario.get();
        }

        Set<Integer> requestedRoleIds = dto.getRoleIds();
        if (requestedRoleIds == null || requestedRoleIds.isEmpty()) {
            Role defaultClientRole = companyRoleProvisioningService
                    .ensureRequiredRoles(companyToUse)
                    .clientPortal();
            requestedRoleIds = Set.of(defaultClientRole.getId());
        }
        replaceClientRoles(savedUser, companyIdToUse, requestedRoleIds);

        java.util.Optional<Apoderado> existente = apoderadoRepository.findByUserIdAndCompanyId(savedUser.getId(), companyIdToUse);
        if (existente.isPresent()) {
            Apoderado registrado = existente.get();
            if (Boolean.TRUE.equals(registrado.getEstado())) {
                throw new IllegalArgumentException("Este cliente ya está registrado y activo en esta empresa");
            }
            accountClosureGuard.assertNotSelfClosed(savedUser.getId(), companyIdToUse);
            TipoInactividad tipoRegistrado = registrado.getTipoInactividad() == TipoInactividad.SUSPENSION
                    ? TipoInactividad.SUSPENSION : TipoInactividad.BAJA;
            throw new veterinaria.vargasvet.exception.ClienteInactivoException(
                    tipoRegistrado == TipoInactividad.SUSPENSION
                            ? "Este cliente está suspendido. Para devolverle el acceso usa «Reactivar» en la lista de clientes"
                            : "Este cliente fue dado de baja. Para devolverle el acceso usa «Reactivar» en la lista de clientes",
                    registrado.getId(), tipoRegistrado);
        }
        Apoderado apoderado = new Apoderado();
        // Si ya existia como identidad EN ESTA EMPRESA (ej. ya es empleado aqui) pero
        // esta es su primera vez como cliente aqui, se le avisa por correo - de otro
        // modo no tiene forma de saber que ahora tambien tiene acceso como cliente, con
        // el mismo usuario y contraseña que ya usa en esta empresa.
        if (!credencialRepository.existsByUsuarioIdAndCompanyId(savedUser.getId(), companyIdToUse)) {
            // Cada empresa tiene su propia credencial - aunque savedUser ya exista, su
            // primera relación con ESTA empresa recibe una contraseña temporal propia,
            // nunca la que ya usa en otra empresa.
            String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                    new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
            credencial.setUsuario(savedUser);
            credencial.setCompany(companyToUse);
            credencial.setPassword(passwordEncoder.encode(tempPassword));
            credencial.setPasswordChanged(false);
            credencial.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
            credencialRepository.save(credencial);
        }
        apoderado.setUser(savedUser);
        apoderado.setCompany(companyToUse);
        apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        apoderado.setNumeroDocumento(dto.getNumeroDocumento());
        apoderado.setGenero(dto.getGenero());
        apoderado.setReferencias(dto.getReferencias());
        apoderado.setObservaciones(dto.getObservaciones());
        apoderado.setEstado(true);
        apoderado.setFechaIngreso(veterinaria.vargasvet.util.AppClock.today());

        Apoderado savedApoderado = apoderadoRepository.save(apoderado);
        companyMembershipService.syncLegacyCompanyField(savedUser);
        contactoService.actualizar(savedUser, companyToUse, dto.getTelefono(), dto.getDireccion());
        consentimientoDatosService.registrarAlta(savedUser, companyIdToUse, dto.getConsentimientoRecordatorios(),
                SecurityUtils.getCurrentUserId());

        // El alta del cliente solo registra sus datos. La invitacion para configurar
        // el acceso se envia cuando la clinica registra su primera mascota. De este
        // modo no se crean accesos utilizables para contactos sin pacientes asociados.

        auditLogService.log(
            "CREAR_APODERADO",
            "Clientes",
            "Se registró al cliente/apoderado " + dto.getNombre() + " " + dto.getApellido() + " con email " + dto.getEmail()
        );

        UserProfileDTO profileDTO = userMapper.toProfileDTO(savedUser);
        profileDTO.setApoderadoId(savedApoderado.getId().intValue());
        profileDTO.setTelefono(contactoService.telefono(savedUser.getId(), companyIdToUse));
        profileDTO.setDireccion(contactoService.direccion(savedUser.getId(), companyIdToUse));
        return profileDTO;
    }

    @Override
    @Transactional
    public void enviarInvitacionAccesoSiTieneMascota(Long apoderadoId) {
        Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado"));

        if (!Boolean.TRUE.equals(apoderado.getEstado())
                || !mascotaRepository.existsByApoderadoIdAndActivoTrue(apoderadoId)) {
            return;
        }
        enviarInvitacion(apoderado);
    }

    @Override
    @Transactional
    public void invitarAcceso(Long apoderadoId) {
        Apoderado apoderado = findAccessibleClient(apoderadoId);
        if (!Boolean.TRUE.equals(apoderado.getEstado())) {
            throw new IllegalArgumentException("No se puede invitar a una persona inactiva");
        }
        enviarInvitacion(apoderado);
    }

    private void enviarInvitacion(Apoderado apoderado) {
        Usuario usuario = apoderado.getUser();
        if (usuario == null) {
            throw new IllegalStateException("El propietario no tiene una cuenta asociada");
        }

        Company company = apoderado.getCompany();
        if (!usuario.isActivo() || !usuario.isEmailVerified()) {
            String verificationToken = SecurityTokenUtils.generate();
            usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
            usuario.setVerificationTokenExpiresAt(
                    veterinaria.vargasvet.util.AppClock.now().plusHours(verificationTokenValidityHours));
            usuarioRepository.save(usuario);
            String nombreCompleto = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                    + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
            sendVerificationEmail(usuario, nombreCompleto, verificationToken, company);
            return;
        }

        sendNewCompanyAccessEmail(usuario, company);
    }

    private java.util.concurrent.CompletableFuture<Boolean> sendVerificationEmail(Usuario usuario, String nombre, String verificationToken, Company company) {
        try {
            String resolvedCompanyName = company != null && company.getName() != null ? company.getName() : defaultCompanyName;
            String resolvedLogo = company != null && company.getLogoUrl() != null ? company.getLogoUrl() : defaultCompanyLogo;
            String resolvedEmail = company != null && company.getEmail() != null ? company.getEmail() : companyEmail;
            String resolvedPhone = company != null && company.getPhone() != null ? company.getPhone() : companyPhone;
            String resolvedAddress = company != null && company.getAddress() != null ? company.getAddress() : companyAddress;
            java.util.Map<String, Object> model = new java.util.HashMap<>();
            model.put("nombre", nombre);
            model.put("email", usuario.getEmail());
            model.put("companyName", resolvedCompanyName);
            model.put("companyLogo", resolvedLogo);
            model.put("companyEmail", resolvedEmail);
            model.put("companyPhone", resolvedPhone);
            model.put("companyAddress", resolvedAddress);
            model.put("verificationLink", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                    "/auth/verify#token=" + verificationToken, company != null ? company.getSlug() : null));
            model.put("avisoPrivacidadLink", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug("/privacidad", company != null ? company.getSlug() : null));

            veterinaria.vargasvet.dto.Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Activa tu cuenta en " + resolvedCompanyName,
                    model
            );

            return emailService.sendEmailWithRetry(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de verificación al apoderado " + usuario.getEmail() + ": " + e.getMessage());
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }
    }

    /** Aviso para cuando una identidad YA existente (encontrada por correo o
     * DNI) se une a una empresa nueva - la persona no tiene forma de saber
     * que ahora tiene acceso aqui tambien si no se le avisa, ya que no pasa
     * por el flujo de activacion de cuenta nueva. */
    private void sendNewCompanyAccessEmail(Usuario usuario, Company company) {
        try {
            String resolvedCompanyName = company.getName() != null ? company.getName() : defaultCompanyName;
            String resolvedLogo = company.getLogoUrl() != null ? company.getLogoUrl() : defaultCompanyLogo;
            String resolvedEmail = company.getEmail() != null ? company.getEmail() : companyEmail;
            String resolvedPhone = company.getPhone() != null ? company.getPhone() : companyPhone;
            java.util.Map<String, Object> model = new java.util.HashMap<>();
            model.put("nombre", (usuario.getNombre() == null ? "" : usuario.getNombre()));
            model.put("username", usuario.getUsername());
            model.put("companyName", resolvedCompanyName);
            model.put("companyLogo", resolvedLogo);
            model.put("companyEmail", resolvedEmail);
            model.put("companyPhone", resolvedPhone);
            model.put("loginUrl", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug("/login", company.getSlug()));

            veterinaria.vargasvet.dto.Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Nuevo acceso en " + resolvedCompanyName,
                    model
            );

            emailService.sendEmailWithRetry(mail, "email/new-company-access-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el aviso de nueva empresa a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto) {
        Apoderado apoderado = findAccessibleClient(id);

        Usuario usuario = apoderado.getUser();

        if (!usuario.isActivo()) {
            throw new IllegalStateException("No se puede editar un cliente inactivo. Active al cliente primero.");
        }
        Integer apoderadoCompanyId = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
        businessValidator.checkCompanyActiva(apoderadoCompanyId);

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (apoderadoCompanyId == null || !apoderadoCompanyId.equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para editar un apoderado de otra empresa");
            }
        }

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
        contactoService.actualizar(usuario, apoderado.getCompany(), dto.getTelefono(), dto.getDireccion());

        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            throw new IllegalArgumentException(
                    "El correo de acceso solo puede modificarse mediante el proceso seguro de doble confirmación");
        }

        usuarioRepository.save(usuario);

        if (dto.getRoleIds() != null) {
            replaceClientRoles(usuario, apoderadoCompanyId, dto.getRoleIds());
        }

        if (dto.getGenero() != null) apoderado.setGenero(dto.getGenero());
        if (dto.getTipoDocumento() != null) apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        if (dto.getReferencias() != null) apoderado.setReferencias(dto.getReferencias());
        if (dto.getObservaciones() != null) apoderado.setObservaciones(dto.getObservaciones());

        apoderado.setUpdatedAt(veterinaria.vargasvet.util.AppClock.now());
        apoderadoRepository.save(apoderado);

        auditLogService.log(
            "ACTUALIZAR_APODERADO",
            "Clientes",
            "Se actualizaron los datos del cliente/apoderado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );

        UserProfileDTO updatedProfile = userMapper.toProfileDTO(usuario);
        updatedProfile.setTelefono(contactoService.telefono(usuario.getId(), apoderadoCompanyId));
        updatedProfile.setDireccion(contactoService.direccion(usuario.getId(), apoderadoCompanyId));
        return updatedProfile;
    }

    @Override
    @Transactional
    public void reenviarInvitacion(Long id) {
        Apoderado apoderado = findAccessibleClient(id);
        Usuario usuario = apoderado.getUser();
        if (!Boolean.TRUE.equals(apoderado.getEstado())) {
            throw new IllegalArgumentException("No se puede reenviar la invitación a un cliente inactivo");
        }
        if (usuario == null) {
            throw new IllegalArgumentException("La cuenta de este cliente ya fue activada");
        }
        entityManager.lock(usuario, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(usuario);
        boolean tieneContrasena = credencialRepository.findAllByUsuarioId(usuario.getId()).stream()
                .anyMatch(veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial::isPasswordChanged);
        if (!veterinaria.vargasvet.util.CuentaPendiente.es(usuario, tieneContrasena)) {
            throw new IllegalArgumentException("La cuenta de este cliente ya fue activada");
        }
        if (!mascotaRepository.existsByApoderadoIdAndActivoTrue(apoderado.getId())
                && !petOwnershipService.tieneVinculoVigente(apoderado)) {
            throw new IllegalArgumentException(
                    "Este cliente aún no tiene mascotas registradas ni está vinculado a ninguna. "
                            + "Se le invita cuando se registre su primera mascota o se le vincule a una");
        }

        java.time.LocalDateTime ahora = veterinaria.vargasvet.util.AppClock.now();
        java.time.LocalDateTime venceActual = usuario.getVerificationTokenExpiresAt();
        if (venceActual != null) {
            java.time.LocalDateTime puedeReenviarDesde = venceActual.minusHours(verificationTokenValidityHours)
                    .plusMinutes(INVITATION_RESEND_COOLDOWN_MINUTES);
            if (puedeReenviarDesde.isAfter(ahora)) {
                long minutos = Math.max(1, (java.time.Duration.between(ahora, puedeReenviarDesde).getSeconds() + 59) / 60);
                throw new IllegalArgumentException("La invitación se envió hace poco. Podrás reenviarla en "
                        + minutos + (minutos == 1 ? " minuto" : " minutos"));
            }
        }

        String verificationToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
        usuario.setVerificationTokenExpiresAt(ahora.plusHours(verificationTokenValidityHours));
        usuarioRepository.save(usuario);
        String nombreCompleto = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        if (veterinaria.vargasvet.util.MailDelivery.failed(
                sendVerificationEmail(usuario, nombreCompleto, verificationToken, apoderado.getCompany()))) {
            throw new veterinaria.vargasvet.exception.MailDeliveryException(
                    "No pudimos enviar el correo de invitación. El enlace anterior sigue vigente; intenta de nuevo en unos minutos");
        }

        Integer companyId = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
        auditLogService.log(companyId, "REENVIAR_INVITACION_CLIENTE", "Clientes",
                "Se reenvió la invitación de activación a " + nombreCompleto + " (" + usuario.getEmail() + ")");
    }

    @Override
    @Transactional
    public ApoderadoEstadoResponse cambiarEstado(Long id, Boolean nuevoEstado, TipoInactividad tipo, String motivo) {
        Apoderado apoderado = findAccessibleClient(id);
        entityManager.lock(apoderado, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(apoderado);

        Usuario usuario = apoderado.getUser();

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (apoderado.getCompany() == null || !apoderado.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para cambiar el estado de un apoderado de otra empresa");
            }
        }

        boolean activar = Boolean.TRUE.equals(nuevoEstado);
        TipoInactividad tipoEfectivo = activar ? null : (tipo != null ? tipo : TipoInactividad.BAJA);
        boolean yaInactivo = !Boolean.TRUE.equals(apoderado.getEstado());

        if (activar && !yaInactivo) {
            return new ApoderadoEstadoResponse();
        }
        if (!activar && yaInactivo && apoderado.getTipoInactividad() == tipoEfectivo
                && apoderado.getFechaModificacionEstado() != null
                && apoderado.getFechaModificacionEstado().isAfter(
                        veterinaria.vargasvet.util.AppClock.now().minusMinutes(STATE_CHANGE_REPEAT_MINUTES))) {
            return new ApoderadoEstadoResponse();
        }

        administratorProtection.assertCanManage(usuario, apoderado.getCompany().getId());
        if (activar) {
            accountClosureGuard.assertNotSelfClosed(usuario.getId(), apoderado.getCompany().getId());
        }
        if (!activar) {
            if (yaInactivo) {
                boolean pasaDeSuspensionABaja = apoderado.getTipoInactividad() == TipoInactividad.SUSPENSION
                        && tipoEfectivo == TipoInactividad.BAJA;
                if (!pasaDeSuspensionABaja) {
                    throw new IllegalArgumentException(apoderado.getTipoInactividad() == TipoInactividad.SUSPENSION
                            ? "El cliente ya está suspendido"
                            : "El cliente ya está dado de baja; solo puede reactivarse");
                }
            } else if (citaRepository.existsCitaVigenteByApoderadoId(apoderado.getId(), veterinaria.vargasvet.util.AppClock.now())) {
                throw new IllegalArgumentException("No se puede desactivar un cliente con citas programadas vigentes");
            }
            if (tipoEfectivo == TipoInactividad.BAJA) {
                assertSinDeuda(apoderado);
            }
        }

        // Solo afecta la relacion con ESTA empresa (apoderado.estado), nunca
        // usuario.activo (login global) - un apoderado puede ser cliente activo de
        // otra empresa a la vez, y desactivarlo aqui no debe bloquearle el acceso ahi.
        apoderado.setEstado(activar);
        apoderado.setTipoInactividad(tipoEfectivo);
        if (tipoEfectivo == TipoInactividad.BAJA) {
            apoderado.setFechaSalida(veterinaria.vargasvet.util.AppClock.today());
        } else {
            apoderado.setFechaSalida(null);
        }
        sessionSecurityService.invalidateSessionsForCompany(usuario, apoderado.getCompany());

        apoderado.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        apoderado.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
        apoderadoRepository.save(apoderado);
        companyMembershipService.syncLegacyCompanyField(usuario);

        ApoderadoEstadoResponse mascotas = petOwnershipService.syncPets(apoderado, tipoEfectivo);

        String accion = activar ? "REACTIVAR_APODERADO"
                : tipoEfectivo == TipoInactividad.SUSPENSION ? "SUSPENDER_APODERADO" : "DAR_DE_BAJA_APODERADO";
        String hecho = activar ? "Se reactivó" : tipoEfectivo == TipoInactividad.SUSPENSION ? "Se suspendió" : "Se dio de baja";
        auditLogService.log(
            apoderado.getCompany() != null ? apoderado.getCompany().getId() : null,
            accion,
            "Clientes",
            hecho + " al cliente/apoderado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
                    + veterinaria.vargasvet.util.AuditDetails.reasonSuffix(motivo)
                    + resumenMascotas(mascotas)
        );
        if (activar) {
            accessRestoredNotifier.send(usuario, apoderado.getCompany());
        }
        return mascotas;
    }

    private void assertSinDeuda(Apoderado apoderado) {
        java.math.BigDecimal deuda = citaRepository.saldoPendienteByApoderadoId(apoderado.getId());
        if (deuda != null && deuda.signum() > 0) {
            throw new IllegalArgumentException("No se puede dar de baja a un cliente con deuda pendiente de S/ "
                    + deuda.setScale(2, java.math.RoundingMode.HALF_UP) + ". Registra su pago antes de darlo de baja.");
        }
    }

    private String resumenMascotas(ApoderadoEstadoResponse mascotas) {
        return ". Mascotas pausadas: " + mascotas.getMascotasPausadas().size()
                + ", restauradas: " + mascotas.getMascotasRestauradas().size()
                + ", que siguen activas por otro autorizador: " + mascotas.getMascotasQueSiguenActivas().size();
    }

    @Override
    @Transactional
    public ApoderadoEstadoResponse eliminar(Long id) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede eliminar a un cliente");
        }
        return cambiarEstado(id, false, TipoInactividad.BAJA, "Eliminado por el administrador");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim().replaceAll("\\s+", " ") : null;
        String docFiltro = (numeroDocumento != null && !numeroDocumento.isBlank()) ? numeroDocumento.trim() : null;
        Page<Apoderado> resultado = apoderadoRepository.buscar(resolvedCompanyId, nombreFiltro, docFiltro,
                PageRequest.of(page, size, Sort.unsorted()));
        java.util.List<Integer> userIds = resultado.getContent().stream()
                .map(Apoderado::getUser).filter(java.util.Objects::nonNull).map(Usuario::getId).toList();
        java.util.List<Long> apoderadoIds = resultado.getContent().stream().map(Apoderado::getId).toList();
        java.util.Set<Integer> conContrasena = userIds.isEmpty()
                ? java.util.Set.of() : credencialRepository.usuariosConContrasenaCreada(userIds);
        java.util.Set<Long> conMascota = apoderadoIds.isEmpty()
                ? java.util.Set.of() : mascotaRepository.apoderadosConMascotaActiva(apoderadoIds);
        java.util.Set<Integer> informados = consentimientoDatosService.usuariosInformados(userIds, resolvedCompanyId);
        return resultado.map(a -> toListResponse(a, conContrasena, conMascota, informados));
    }

    private Integer resolverCompanyId(Integer companyIdParam) {
        if (SecurityUtils.isSuperAdmin()) {
            if (companyIdParam == null) {
                throw new IllegalArgumentException("El parámetro companyId es requerido para SUPER_ADMIN");
            }
            return companyIdParam;
        }
        return SecurityUtils.getCurrentCompanyId();
    }

    @Override
    @Transactional(readOnly = true)
    public ApoderadoRequest findById(Long id) {
        Apoderado apoderado = findAccessibleClient(id);

        Usuario usuario = apoderado.getUser();
        ApoderadoRequest dto = new ApoderadoRequest();
        dto.setId(apoderado.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setNumeroDocumento(usuario.getDni());
        Integer apoderadoCompanyIdForDto = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
        dto.setTelefono(contactoService.telefono(usuario.getId(), apoderadoCompanyIdForDto));
        dto.setDireccion(contactoService.direccion(usuario.getId(), apoderadoCompanyIdForDto));
        dto.setCompanyId(apoderadoCompanyIdForDto);
        dto.setRoleIds(usuario.getUsuariosPorRol() == null
                ? Set.of()
                : usuario.getUsuariosPorRol().stream()
                    .filter(assignment -> assignment.getRol() != null
                            && assignment.getRol().getScope() == RoleScope.CLIENT
                            && assignment.getCompany() != null
                            && assignment.getCompany().getId().equals(apoderadoCompanyIdForDto))
                    .map(assignment -> assignment.getRol().getId())
                    .collect(java.util.stream.Collectors.toSet()));

        dto.setGenero(apoderado.getGenero());
        dto.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        dto.setReferencias(apoderado.getReferencias());
        dto.setObservaciones(apoderado.getObservaciones());

        return dto;
    }

    /** companyId se recibe explicito (no se deriva de usuario.getCompany()) porque un
     * Apoderado puede estar activo en varias empresas a la vez - Usuario.company es
     * solo una cache que queda en null apenas hay ambiguedad. Solo se tocan las
     * asignaciones CLIENT de ESTA empresa; nunca las de otras empresas del mismo
     * usuario. */
    private void replaceClientRoles(Usuario usuario, Integer companyId, Set<Integer> requestedRoleIds) {
        if (requestedRoleIds == null || requestedRoleIds.isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un rol de cliente");
        }

        Set<Integer> uniqueRoleIds = new HashSet<>(requestedRoleIds);
        List<Role> roles = roleRepository.findAllById(uniqueRoleIds);
        if (roles.size() != uniqueRoleIds.size()) {
            throw new IllegalArgumentException("Uno o más roles seleccionados no existen");
        }

        boolean invalidRole = roles.stream().anyMatch(role -> !role.isActivo()
                || role.getScope() != RoleScope.CLIENT
                || role.getCompany() == null
                || !companyId.equals(role.getCompany().getId()));
        if (invalidRole) {
            throw new IllegalArgumentException("Solo puede asignar roles de cliente activos de la misma empresa");
        }

        usuario.getUsuariosPorRol().removeIf(assignment -> assignment.getRol() != null
                && assignment.getRol().getScope() == RoleScope.CLIENT
                && assignment.getCompany() != null
                && companyId.equals(assignment.getCompany().getId())
                && !uniqueRoleIds.contains(assignment.getRol().getId()));
        Set<Integer> alreadyAssigned = usuario.getUsuariosPorRol().stream()
                .filter(assignment -> assignment.getRol() != null
                        && assignment.getCompany() != null
                        && companyId.equals(assignment.getCompany().getId()))
                .map(assignment -> assignment.getRol().getId())
                .collect(java.util.stream.Collectors.toSet());

        roles.stream().filter(role -> !alreadyAssigned.contains(role.getId())).map(role -> {
            UsuarioPorRol assignment = new UsuarioPorRol();
            assignment.setUsuario(usuario);
            assignment.setRol(role);
            assignment.setCompany(role.getCompany());
            return assignment;
        }).forEach(usuario.getUsuariosPorRol()::add);
        usuarioRepository.save(usuario);
    }

    private Apoderado findAccessibleClient(Long clientId) {
        if (SecurityUtils.isSuperAdmin()) {
            return apoderadoRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        }

        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }
        return apoderadoRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private ApoderadoListResponse toListResponse(Apoderado apoderado, java.util.Set<Integer> conContrasena, java.util.Set<Long> conMascota,
                                                 java.util.Set<Integer> informados) {
        ApoderadoListResponse response = new ApoderadoListResponse();
        response.setId(apoderado.getId());
        response.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        response.setNumeroDocumento(apoderado.getNumeroDocumento());
        if (apoderado.getUser() != null) {
            response.setUserId(apoderado.getUser().getId());
            response.setNombre(apoderado.getUser().getNombre());
            response.setApellido(apoderado.getUser().getApellido());
            response.setEmail(apoderado.getUser().getEmail());
            Integer listCompanyId = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
            response.setTelefono(contactoService.telefono(apoderado.getUser().getId(), listCompanyId));
            response.setActivo(apoderado.getEstado());
            response.setTipoInactividad(apoderado.getTipoInactividad());
            boolean pendiente = Boolean.TRUE.equals(apoderado.getEstado())
                    && veterinaria.vargasvet.util.CuentaPendiente.es(apoderado.getUser(), conContrasena.contains(apoderado.getUser().getId()));
            response.setCuentaPendiente(pendiente);
            response.setPuedeReenviarInvitacion(pendiente && conMascota.contains(apoderado.getId()));
            response.setAvisoInformado(informados.contains(apoderado.getUser().getId()));
        }
        return response;
    }
}
