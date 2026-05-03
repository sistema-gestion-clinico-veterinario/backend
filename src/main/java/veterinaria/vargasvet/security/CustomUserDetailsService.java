package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (usuario.getApoderado() != null && usuario.getEmpleadoVeterinario() == null && usuario.getRoles().isEmpty()) {
            throw new UsernameNotFoundException("Los apoderados no tienen acceso al sistema");
        }

        List<GrantedAuthority> authorities;
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            authorities = usuario.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                    .collect(Collectors.toList());
        } else {
            authorities = Collections.emptyList();
        }

        return new User(usuario.getEmail(), usuario.getPassword(), authorities);
    }
}
