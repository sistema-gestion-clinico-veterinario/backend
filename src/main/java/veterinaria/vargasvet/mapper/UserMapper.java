package veterinaria.vargasvet.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.domain.entity.EmpleadoVeterinario;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

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

        EmpleadoVeterinario empleado = new EmpleadoVeterinario();
        empleado.setNumeroDocumentoIdentidad(dto.getDni());
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado.setUser(usuario);

        usuario.setEmpleadoVeterinario(empleado);
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
                    .map(r -> r.getName().name())
                    .collect(Collectors.joining(",")));
        }

        if (usuario.getCompany() != null) {
            dto.setCompanyId(usuario.getCompany().getId());
            dto.setCompanyName(usuario.getCompany().getName());
        }

        return dto;
    }
}
