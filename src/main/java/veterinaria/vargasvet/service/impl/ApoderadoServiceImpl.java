package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
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
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApoderadoServiceImpl implements ApoderadoService {

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

        // Misma persona real si coincide el correo O el DNI - alguien puede
        // haberse registrado antes en otra empresa con un correo distinto
        // (ej. de trabajo) al que usa aqui; bloquearlo solo porque el DNI ya
        // existe seria un callejon sin salida (no se puede crear una
        // identidad nueva, pero tampoco se reconoce la existente). Se
        // reutiliza la identidad encontrada por cualquiera de los dos.
        java.util.Optional<Usuario> existingUsuario = usuarioRepository.findByEmail(dto.getEmail())
                .or(() -> usuarioRepository.findByDni(dto.getNumeroDocumento()));
        boolean esUsuarioNuevo = existingUsuario.isEmpty();
        Usuario savedUser;
        String verificationToken = null;

        if (esUsuarioNuevo) {
            String username = dto.getUsername() == null ? null : dto.getUsername().trim().toLowerCase(java.util.Locale.ROOT);
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("El usuario es obligatorio para una persona nueva");
            }
            if (usuarioRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("El usuario ya está en uso");
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setNombre(dto.getNombre());
            usuario.setApellido(dto.getApellido());
            usuario.setEmail(dto.getEmail());
            usuario.setDni(dto.getNumeroDocumento());
            usuario.setTelefono(dto.getTelefono());
            usuario.setDireccion(dto.getDireccion());
            String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            usuario.setPassword(passwordEncoder.encode(tempPassword));
            usuario.setActivo(false);
            usuario.setEmailVerified(false);
            verificationToken = SecurityTokenUtils.generate();
            usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
            usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(verificationTokenValidityHours));

            savedUser = usuarioRepository.save(usuario);
        } else {
            // Email ya existente: la misma persona se registra como cliente de OTRA
            // empresa (o de la misma, ver reactivacion abajo) - permitido a proposito,
            // a diferencia de Empleado un Apoderado si puede estar activo en varias
            // empresas a la vez. Sin chequeo de conflicto.
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

        // Reingreso a la MISMA empresa: reactiva la fila existente en vez de crear una
        // nueva (numero_documento se mantiene reservado por empresa incluso inactivo -
        // ver uq_apoderado_documento_empresa - asi que insertar una segunda fila
        // chocaria con el indice). Esto ademas preserva mascotas/historial ya asociados
        // a esa fila, que se perderian de vista si se creara una fila nueva.
        Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(savedUser.getId(), companyIdToUse)
                .orElseGet(Apoderado::new);
        if (apoderado.getId() != null && Boolean.TRUE.equals(apoderado.getEstado())) {
            throw new IllegalArgumentException("Este cliente ya está registrado y activo en esta empresa");
        }
        // Si ya existia como identidad pero esta es su primera relacion con
        // ESTA empresa en particular, se le avisa por correo - de otro modo
        // no tiene forma de saber que ahora tambien tiene acceso aqui (con el
        // mismo usuario y contraseña que ya usa en sus otras empresas).
        boolean esNuevaEmpresaParaEsteUsuario = apoderado.getId() == null;
        apoderado.setUser(savedUser);
        apoderado.setCompany(companyToUse);
        apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        apoderado.setNumeroDocumento(dto.getNumeroDocumento());
        apoderado.setGenero(dto.getGenero());
        apoderado.setReferencias(dto.getReferencias());
        apoderado.setObservaciones(dto.getObservaciones());
        apoderado.setEstado(true);
        apoderado.setFechaSalida(null);
        if (apoderado.getFechaIngreso() == null) {
            apoderado.setFechaIngreso(veterinaria.vargasvet.util.AppClock.today());
        }

        Apoderado savedApoderado = apoderadoRepository.save(apoderado);
        companyMembershipService.syncLegacyCompanyField(savedUser);

        if (esUsuarioNuevo) {
            sendVerificationEmail(savedUser, dto.getNombre() + " " + dto.getApellido(), verificationToken);
        } else if (esNuevaEmpresaParaEsteUsuario) {
            sendNewCompanyAccessEmail(savedUser, companyToUse);
        }

        auditLogService.log(
            "CREAR_APODERADO",
            "Clientes",
            "Se registró al cliente/apoderado " + dto.getNombre() + " " + dto.getApellido() + " con email " + dto.getEmail()
        );

        UserProfileDTO profileDTO = userMapper.toProfileDTO(savedUser);
        profileDTO.setApoderadoId(savedApoderado.getId().intValue());
        return profileDTO;
    }

    private void sendVerificationEmail(Usuario usuario, String nombre, String verificationToken) {
        try {
            Company company = usuario.getCompany();
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

            veterinaria.vargasvet.dto.Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Activa tu cuenta en " + resolvedCompanyName,
                    model
            );

            emailService.sendEmailWithRetry(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de verificación al apoderado " + usuario.getEmail() + ": " + e.getMessage());
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
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());

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

        return userMapper.toProfileDTO(usuario);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean nuevoEstado) {
        Apoderado apoderado = findAccessibleClient(id);

        Usuario usuario = apoderado.getUser();


        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (apoderado.getCompany() == null || !apoderado.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para cambiar el estado de un apoderado de otra empresa");
            }
        }

        if (Boolean.FALSE.equals(nuevoEstado)
                && citaRepository.existsCitaVigenteByApoderadoId(apoderado.getId(), veterinaria.vargasvet.util.AppClock.now())) {
            throw new IllegalArgumentException("No se puede desactivar un cliente con citas programadas vigentes");
        }

        // Solo afecta la relacion con ESTA empresa (apoderado.estado), nunca
        // usuario.activo (login global) - un apoderado puede ser cliente activo de
        // otra empresa a la vez, y desactivarlo aqui no debe bloquearle el acceso ahi.
        apoderado.setEstado(nuevoEstado);
        if (Boolean.FALSE.equals(nuevoEstado)) {
            apoderado.setFechaSalida(veterinaria.vargasvet.util.AppClock.today());
        } else {
            apoderado.setFechaSalida(null);
        }
        sessionSecurityService.invalidateSessionsForCompany(usuario, apoderado.getCompany());

        apoderado.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        apoderado.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
        apoderadoRepository.save(apoderado);
        companyMembershipService.syncLegacyCompanyField(usuario);


        // Al desactivar, se da de baja en cascada a las mascotas del apoderado (con motivo
        // y auditoria propios, igual que el flujo individual de MascotaServiceImpl). Al
        // reactivar, en cambio, NO se reactivan las mascotas automaticamente: una mascota
        // pudo quedar inactiva por una causa propia y no relacionada (fallecimiento, cambio
        // de propietario), y reactivar al apoderado no debe revertir eso silenciosamente.
        if (Boolean.FALSE.equals(nuevoEstado)) {
            List<Mascota> mascotas = mascotaRepository.findByApoderadoId(apoderado.getId());
            for (Mascota mascota : mascotas) {
                if (!Boolean.TRUE.equals(mascota.getActivo())) continue;
                mascota.setActivo(false);
                mascota.setMotivoBaja(veterinaria.vargasvet.domain.enums.MotivoBajaMascota.DEJA_ASISTIR);
                mascota.setOtroMotivoBaja(null);
                mascota.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
                mascota.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
                mascotaRepository.save(mascota);
            }
        }

        auditLogService.log(
            Boolean.TRUE.equals(nuevoEstado) ? "ACTIVAR_APODERADO" : "DESACTIVAR_APODERADO",
            "Clientes",
            (Boolean.TRUE.equals(nuevoEstado) ? "Se activó" : "Se desactivó") + " al cliente/apoderado " + usuario.getNombre() + " " + usuario.getApellido() + " (" + usuario.getEmail() + ")"
        );
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Apoderado apoderado = findAccessibleClient(id);
        if (!mascotaRepository.findByApoderadoId(apoderado.getId()).isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un propietario que tiene mascotas registradas");
        }
        Usuario usuario = apoderado.getUser();
        String clientNombre = usuario.getNombre() + " " + usuario.getApellido();
        String clientEmail = usuario.getEmail();
        refreshTokenRepository.deleteByUsuario(usuario);
        apoderadoRepository.delete(apoderado);
        usuarioRepository.delete(usuario);

        auditLogService.log(
            "ELIMINAR_APODERADO",
            "Clientes",
            "Se eliminó permanentemente al cliente/apoderado " + clientNombre + " (" + clientEmail + ")"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim().replaceAll("\\s+", " ") : null;
        String docFiltro = (numeroDocumento != null && !numeroDocumento.isBlank()) ? numeroDocumento.trim() : null;
        return apoderadoRepository.buscar(resolvedCompanyId, nombreFiltro, docFiltro,
                PageRequest.of(page, size, Sort.unsorted()))
                .map(this::toListResponse);
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
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        Integer apoderadoCompanyIdForDto = apoderado.getCompany() != null ? apoderado.getCompany().getId() : null;
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
                && companyId.equals(assignment.getCompany().getId()));

        roles.stream().map(role -> {
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

    private ApoderadoListResponse toListResponse(Apoderado apoderado) {
        ApoderadoListResponse response = new ApoderadoListResponse();
        response.setId(apoderado.getId());
        response.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        response.setNumeroDocumento(apoderado.getNumeroDocumento());
        if (apoderado.getUser() != null) {
            response.setNombre(apoderado.getUser().getNombre());
            response.setApellido(apoderado.getUser().getApellido());
            response.setEmail(apoderado.getUser().getEmail());
            response.setTelefono(apoderado.getUser().getTelefono());
            response.setActivo(apoderado.getEstado());
        }
        return response;
    }
}
