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

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (usuario.getApoderado() != null && usuario.getEmpleadoVeterinario() == null && usuario.getRole() == null) {
            throw new UsernameNotFoundException("Los apoderados no tienen acceso al sistema");
        }

        List<GrantedAuthority> authorities;
        if (usuario.getRole() != null) {
            authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(usuario.getRole().getName().name())
            );
        } else {
            authorities = Collections.emptyList();
        }

        return new User(usuario.getEmail(), usuario.getPassword(), authorities);
    }
}
