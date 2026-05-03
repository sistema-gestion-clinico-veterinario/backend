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
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            Set<GrantedAuthority> authSet = new HashSet<>();
            for (Role role : usuario.getRoles()) {
                // Agregar el rol con prefijo ROLE_
                authSet.add(new SimpleGrantedAuthority(role.getName()));
                
                // Agregar todos los permisos asociados al rol
                if (role.getPermissions() != null) {
                    for (Permission perm : role.getPermissions()) {
                        authSet.add(new SimpleGrantedAuthority(perm.getName()));
                    }
                }
            }
            authorities = new ArrayList<>(authSet);
        } else {
            authorities = Collections.emptyList();
        }

        return new User(usuario.getEmail(), usuario.getPassword(), authorities);
    }
}
