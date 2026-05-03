package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.enums.ERole;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.TokenProvider;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements veterinaria.vargasvet.service.UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;

    @Override
    @Transactional
    public UserProfileDTO register(UserRegistrationDTO registrationDTO) {
        if (usuarioRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        Usuario usuario = userMapper.toEntity(registrationDTO);
        
        roleRepository.findByName(ERole.ROLE_VETERINARIO)
                .ifPresent(usuario::setRole);

        Usuario saved = usuarioRepository.save(usuario);
        return userMapper.toProfileDTO(saved);
    }

    @Override
    public AuthResponse login(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!usuario.isActivo()) {
            throw new DisabledException("La cuenta está suspendida");
        }

        if (usuario.getApoderado() != null && usuario.getEmpleadoVeterinario() == null && usuario.getRole() == null) {
            throw new BadCredentialsException("Los apoderados no tienen acceso al sistema");
        }

        String systemRole = usuario.getRole() != null ? usuario.getRole().getName().name() : null;
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        
        List<String> permissions = List.of();
        if (usuario.getRole() != null && usuario.getRole().getPermissions() != null) {
            permissions = usuario.getRole().getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toList());
        }

        String jwt = tokenProvider.createToken(usuario.getEmail(), systemRole, companyId, permissions);

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setSystemRole(systemRole);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setPermissions(permissions);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));

        return response;
    }

    @Override
    public UserProfileDTO getProfile(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return userMapper.toProfileDTO(usuario);
    }

    @Override
    public void suspendAccount(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private String resolveNombreCompleto(Usuario usuario) {
        if (usuario.getEmpleadoVeterinario() != null) {
            return usuario.getEmpleadoVeterinario().getNombre() + " " + usuario.getEmpleadoVeterinario().getApellido();
        }
        return usuario.getEmail();
    }

    private String resolveUserType(Usuario usuario) {
        if (usuario.getRole() != null && usuario.getRole().getName() == ERole.ROLE_SUPER_ADMIN) return "SUPER_ADMIN";
        if (usuario.getEmpleadoVeterinario() != null) return "EMPLEADO";
        return "USUARIO";
    }
}
