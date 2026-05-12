package veterinaria.vargasvet.modules.users.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.modules.users.domain.entity.Empleado;
import veterinaria.vargasvet.modules.users.domain.entity.Usuario;
import veterinaria.vargasvet.modules.users.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;
import veterinaria.vargasvet.modules.users.domain.enums.Genero;
import veterinaria.vargasvet.modules.users.domain.enums.TipoDocumentoIdentidad;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public Usuario toEntity(UserRegistrationDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(dto.getPassword());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setDni(dto.getDni());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setActivo(true);
        return usuario;
    }

    public Usuario toEmpleadoEntity(UserRegistrationDTO dto) {
        Usuario usuario = toEntity(dto);

        Empleado empleado = new Empleado();
        empleado.setNumeroDocumentoIdentidad(dto.getDni());
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado.setUser(usuario);

        usuario.setEmpleado(empleado);
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

        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            dto.setSystemRole(usuario.getRoles().stream()
                    .map(r -> r.getName())
                    .collect(Collectors.joining(",")));
        }

        if (usuario.getCompany() != null) {
            dto.setCompanyId(usuario.getCompany().getId());
            dto.setCompanyName(usuario.getCompany().getName());
        }

        dto.setActivo(usuario.isActivo());

        return dto;
    }
}
