package veterinaria.vargasvet.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public Usuario toEntity(UserRegistrationDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setUsername(dto.getUsername());
        usuario.setPassword(dto.getPassword());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setDni(dto.getDni());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setActivo(true);
        return usuario;
    }

    public UserProfileDTO toProfileDTO(Usuario usuario) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setDni(usuario.getDni());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());

        if (usuario.getUsuariosPorRol() != null && !usuario.getUsuariosPorRol().isEmpty()) {
            dto.setSystemRole(usuario.getUsuariosPorRol().stream()
                    .map(upr -> upr.getRol().getName())
                    .collect(Collectors.joining(",")));
            dto.setRoleIds(usuario.getUsuariosPorRol().stream()
                    .map(upr -> upr.getRol().getId())
                    .collect(Collectors.toSet()));
        }

        if (usuario.getCompany() != null) {
            dto.setCompanyId(usuario.getCompany().getId());
            dto.setCompanyName(usuario.getCompany().getName());
        }

        dto.setActivo(usuario.isActivo());

        return dto;
    }
}
