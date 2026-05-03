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

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final ModelMapper modelMapper;

    public Usuario toEntity(UserRegistrationDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(dto.getPassword());
        usuario.setActivo(true);
        
        EmpleadoVeterinario empleado = new EmpleadoVeterinario();
        empleado.setNombre(dto.getNombre());
        empleado.setApellido(dto.getApellido());
        empleado.setNumeroDocumentoIdentidad(dto.getDni());
        empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
        empleado.setGenero(Genero.MASCULINO);
        empleado.setEstado(true);
        empleado.setTelefono(dto.getTelefono());
        empleado.setDireccion(dto.getDireccion());
        empleado.setUser(usuario);
        
        usuario.setEmpleadoVeterinario(empleado);
        return usuario;
    }

    public UserProfileDTO toProfileDTO(Usuario usuario) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        
        if (usuario.getEmpleadoVeterinario() != null) {
            dto.setNombre(usuario.getEmpleadoVeterinario().getNombre());
            dto.setApellido(usuario.getEmpleadoVeterinario().getApellido());
            dto.setDni(usuario.getEmpleadoVeterinario().getNumeroDocumentoIdentidad());
            dto.setTelefono(usuario.getEmpleadoVeterinario().getTelefono());
            dto.setDireccion(usuario.getEmpleadoVeterinario().getDireccion());
        }
        
        if (usuario.getRole() != null) {
            dto.setSystemRole(usuario.getRole().getName().name());
        }
        
        if (usuario.getCompany() != null) {
            dto.setCompanyId(usuario.getCompany().getId());
            dto.setCompanyName(usuario.getCompany().getName());
        }
        
        return dto;
    }
}
