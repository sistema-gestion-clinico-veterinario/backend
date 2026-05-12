package veterinaria.vargasvet.pacientes;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.pacientes.Apoderado;
import veterinaria.vargasvet.admin.Role;
import veterinaria.vargasvet.admin.Usuario;
import veterinaria.vargasvet.pacientes.Mascota;
import veterinaria.vargasvet.pacientes.ApoderadoRequest;
import veterinaria.vargasvet.admin.UserProfileDTO;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.admin.UserMapper;
import veterinaria.vargasvet.pacientes.ApoderadoRepository;
import veterinaria.vargasvet.admin.CompanyRepository;
import veterinaria.vargasvet.pacientes.MascotaRepository;
import veterinaria.vargasvet.admin.RoleRepository;
import veterinaria.vargasvet.admin.UsuarioRepository;
import veterinaria.vargasvet.shared.SecurityUtils;
import veterinaria.vargasvet.pacientes.ApoderadoService;
import veterinaria.vargasvet.shared.BusinessValidator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.pacientes.ApoderadoListResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApoderadoServiceImpl implements ApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final BusinessValidator businessValidator;

    @Override
    @Transactional
    public UserProfileDTO registerApoderado(ApoderadoRequest dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (usuarioRepository.existsByDni(dto.getNumeroDocumento())) {
            throw new IllegalArgumentException("El DNI ya está registrado en el sistema");
        }

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

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setDni(dto.getNumeroDocumento());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setActivo(true);
        usuario.setEmailVerified(true);
        businessValidator.checkCompanyActiva(companyIdToUse);
        usuario.setCompany(companyRepository.findById(companyIdToUse)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));

        Role apoderadoRole = roleRepository.findByName("ROLE_CLIENTE")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Error de configuración: El rol 'ROLE_CLIENTE' no existe."));
        usuario.getRoles().add(apoderadoRole);

        Usuario savedUser = usuarioRepository.save(usuario);

        Apoderado apoderado = new Apoderado();
        apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        apoderado.setNumeroDocumento(dto.getNumeroDocumento());
        apoderado.setGenero(dto.getGenero());
        apoderado.setReferencias(dto.getReferencias());
        apoderado.setObservaciones(dto.getObservaciones());
        apoderado.setUser(savedUser);

        apoderadoRepository.save(apoderado);

        return userMapper.toProfileDTO(savedUser);
    }

    @Override
    @Transactional
    public UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();

        if (!usuario.isActivo()) {
            throw new IllegalStateException("No se puede editar un cliente inactivo. Active al cliente primero.");
        }
        businessValidator.checkCompanyActiva(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para editar un apoderado de otra empresa");
            }
        }

        if (dto.getNombre() != null) usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null) usuario.setApellido(dto.getApellido());
        if (dto.getTelefono() != null) usuario.setTelefono(dto.getTelefono());
        if (dto.getDireccion() != null) usuario.setDireccion(dto.getDireccion());

        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado por otro usuario");
            }
            usuario.setEmail(dto.getEmail());
        }

        usuarioRepository.save(usuario);

        if (dto.getGenero() != null) apoderado.setGenero(dto.getGenero());
        if (dto.getTipoDocumento() != null) apoderado.setTipoDocumentoIdentidad(dto.getTipoDocumento());
        if (dto.getReferencias() != null) apoderado.setReferencias(dto.getReferencias());
        if (dto.getObservaciones() != null) apoderado.setObservaciones(dto.getObservaciones());

        apoderado.setUpdatedAt(LocalDateTime.now());
        apoderadoRepository.save(apoderado);

        return userMapper.toProfileDTO(usuario);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, Boolean nuevoEstado) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();


        Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin()) {
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para cambiar el estado de un apoderado de otra empresa");
            }
        }


        usuario.setActivo(nuevoEstado);
        usuarioRepository.save(usuario);

        apoderado.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
        apoderado.setFechaModificacionEstado(LocalDateTime.now());
        apoderadoRepository.save(apoderado);


        List<Mascota> mascotas = mascotaRepository.findByApoderadoId(apoderado.getId());
        for (Mascota mascota : mascotas) {
            mascota.setActivo(nuevoEstado);
            mascotaRepository.save(mascota);
        }
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));
        if (!mascotaRepository.findByApoderadoId(apoderado.getId()).isEmpty()) {
            throw new IllegalArgumentException("No se puede eliminar un propietario que tiene mascotas registradas");
        }
        apoderadoRepository.delete(apoderado);
        usuarioRepository.delete(apoderado.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size) {
        Integer resolvedCompanyId = resolverCompanyId(companyId);
        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? nombre.trim() : null;
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
        Apoderado apoderado = apoderadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apoderado no encontrado con ID: " + id));

        Usuario usuario = apoderado.getUser();
        ApoderadoRequest dto = new ApoderadoRequest();
        dto.setId(apoderado.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setNumeroDocumento(usuario.getDni());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        dto.setCompanyId(usuario.getCompany() != null ? usuario.getCompany().getId() : null);

        dto.setGenero(apoderado.getGenero());
        dto.setTipoDocumento(apoderado.getTipoDocumentoIdentidad());
        dto.setReferencias(apoderado.getReferencias());
        dto.setObservaciones(apoderado.getObservaciones());

        return dto;
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
            response.setActivo(apoderado.getUser().isActivo());
        }
        return response;
    }
}
